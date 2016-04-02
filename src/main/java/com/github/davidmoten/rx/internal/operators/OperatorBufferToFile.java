package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.github.davidmoten.rx.buffertofile.CacheType;
import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.buffertofile.Options;
import com.github.davidmoten.rx.internal.operators.RollingQueue.Queue2;
import com.github.davidmoten.util.Preconditions;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.internal.operators.BackpressureUtils;
import rx.observers.Subscribers;

public final class OperatorBufferToFile<T> implements Operator<T, T> {

	private static final String QUEUE_NAME = "q";

	private final Serializer<T> serializer;
	private final Scheduler scheduler;
	private final Options options;

	public OperatorBufferToFile(DataSerializer<T> dataSerializer, Scheduler scheduler, Options options) {
		Preconditions.checkNotNull(dataSerializer);
		Preconditions.checkNotNull(scheduler);
		Preconditions.checkNotNull(options);
		this.scheduler = scheduler;
		this.serializer = createSerializer(dataSerializer);
		this.options = options;
	}

	private static <T> Serializer<T> createSerializer(DataSerializer<T> dataSerializer) {
		return new MapDbSerializer<T>(dataSerializer);
	}

	@Override
	public Subscriber<? super T> call(Subscriber<? super T> child) {

		// create the MapDB queue
		final CloseableQueue<T> queue = createQueue(serializer, options);

		// hold a reference to the queueProducer which will be set on
		// subscription to `source`
		final AtomicReference<QueueProducer<T>> queueProducer = new AtomicReference<QueueProducer<T>>();

		// emissions will propagate to downstream via this worker
		final Worker worker = scheduler.createWorker();

		// set up the observable to read from the MapDB queue
		Observable<T> source = Observable.create(new OnSubscribeFromQueue<T>(queueProducer, queue, worker, options));

		// create the parent subscriber
		Subscriber<T> parentSubscriber = new ParentSubscriber<T>(queueProducer);

		// ensure worker gets unsubscribed
		child.add(worker);

		// link unsubscription
		child.add(parentSubscriber);

		// close and delete database on unsubscription
		child.add(disposer(queue));

		// ensure onStart not called twice
		Subscriber<T> wrappedChild = Subscribers.wrap(child);

		// subscribe to queue
		source.unsafeSubscribe(wrappedChild);

		return parentSubscriber;
	}

	private static class Q2<T> implements Queue2<T> {

		private final DB db;
		private final Queue<T> queue;

		public Q2(DB db, Queue<T> queue) {
			this.db = db;
			this.queue = queue;
		}

		@Override
		public T peek() {
			return queue.peek();
		}

		@Override
		public T poll() {
			return queue.poll();
		}

		@Override
		public boolean offer(T t) {
			return queue.offer(t);
		}

		@Override
		public void dispose() {
			System.out.println("disposing " + db);
			db.close();
			System.out.println("disposed");
		}

		@Override
		public boolean isEmpty() {
			return queue.isEmpty();
		}

	}

	private static <T> CloseableQueue<T> createQueue(final Serializer<T> serializer, final Options options) {
		final Func0<Queue2<T>> queueFactory = new Func0<Queue2<T>>() {
			@Override
			public Queue2<T> call() {
				System.out.println("creating new queue");
				// create the file to be used for queue storage (and whose file
				// name will determine the names of other files used for
				// storage)
				File file = options.fileFactory().call();

				// create a MapDB database using file
				final DB db = createDb(file, options);

				// create the queue
				Queue<T> q = db.createQueue(QUEUE_NAME, serializer, false);

				return new Q2<T>(db, q);
			}
		};
		return new RollingQueue<T>(queueFactory, options.rolloverEvery());
	}

	private static DB createDb(File file, Options options) {
		DBMaker<?> builder = DBMaker.newFileDB(file);
		if (options.cacheType() == CacheType.NO_CACHE) {
			builder = builder.cacheDisable();
		} else if (options.cacheType() == CacheType.HARD_REF) {
			builder = builder.cacheHardRefEnable();
		} else if (options.cacheType() == CacheType.SOFT_REF) {
			builder = builder.cacheSoftRefEnable();
		} else if (options.cacheType() == CacheType.WEAK_REF) {
			builder = builder.cacheWeakRefEnable();
		} else if (options.cacheType() == CacheType.LEAST_RECENTLY_USED) {
			builder = builder.cacheLRUEnable();
		} else {
			throw new RuntimeException("unknown cacheType " + options.cacheType());
		}
		if (options.cacheSizeItems().isPresent()) {
			builder = builder.cacheSize(options.cacheSizeItems().get());
		}
		if (options.storageSizeLimitMB().isPresent()) {
			// sizeLimit is expected in GB
			builder = builder.sizeLimit(options.storageSizeLimitMB().get() / 1024);
		}
		builder = builder.deleteFilesAfterClose();
		return builder.transactionDisable().make();
	}

	private static final class OnSubscribeFromQueue<T> implements OnSubscribe<T> {

		private final AtomicReference<QueueProducer<T>> queueProducer;
		private final CloseableQueue<T> queue;
		private final Worker worker;
		private final Options options;

		OnSubscribeFromQueue(AtomicReference<QueueProducer<T>> queueProducer, CloseableQueue<T> queue, Worker worker,
				Options options) {
			this.queueProducer = queueProducer;
			this.queue = queue;
			this.worker = worker;
			this.options = options;
		}

		@Override
		public void call(Subscriber<? super T> child) {
			QueueProducer<T> qp = new QueueProducer<T>(queue, child, worker, options.delayError());
			queueProducer.set(qp);
			child.setProducer(qp);
		}
	}

	private static final class ParentSubscriber<T> extends Subscriber<T> {

		private final AtomicReference<QueueProducer<T>> queueProducer;

		ParentSubscriber(AtomicReference<QueueProducer<T>> queueProducer) {
			this.queueProducer = queueProducer;
		}

		@Override
		public void onStart() {
			request(Long.MAX_VALUE);
		}

		@Override
		public void onCompleted() {
			queueProducer.get().onCompleted();
		}

		@Override
		public void onError(Throwable e) {
			queueProducer.get().onError(e);
		}

		@Override
		public void onNext(T t) {
			queueProducer.get().onNext(t);
		}

	}

	private static final class QueueProducer<T> extends AtomicLong implements Producer, Action0 {

		private static final long serialVersionUID = 2521533710633950102L;
		private final CloseableQueue<T> queue;
		private final AtomicInteger drainRequested = new AtomicInteger(0);
		private final Subscriber<? super T> child;
		private final Worker worker;
		private final boolean delayError;
		private volatile boolean done = false;
		private volatile Throwable error = null;

		QueueProducer(CloseableQueue<T> queue, Subscriber<? super T> child, Worker worker, boolean delayError) {
			super();
			this.queue = queue;
			this.child = child;
			this.worker = worker;
			this.delayError = delayError;
		}

		void onNext(T t) {
			if (!queue.offer(t)) {
				onError(new RuntimeException(
						"could not place item on queue (queue.offer(item) returned false), item= " + t));
				return;
			} else {
				drain();
			}
		}

		void onError(Throwable e) {
			// must assign error before assign done = true to avoid race
			// condition in finished()
			error = e;
			done = true;
			drain();
		}

		void onCompleted() {
			done = true;
			drain();
		}

		@Override
		public void request(long n) {
			if (n > 0) {
				BackpressureUtils.getAndAddRequest(this, n);
				drain();
			}
		}

		private void drain() {
			// only schedule a drain if current drain has finished
			// otherwise the drainRequested counter will be incremented
			// and the drain loop will ensure that another drain cyle occurs if
			// required
			if (drainRequested.getAndIncrement() == 0) {
				worker.schedule(this);
			}
		}

		// this method executed from drain() only
		@Override
		public void call() {
			// catch exceptions related to MapDB usage in drainNow()
			try {
				drainNow();
			} catch (IOError e) {
				if (e.getCause() != null) {
					// unwrap IOError(IOException: no free space to expand
					// Volume) because API indicates that IOException will be
					// emitted in the case of storage overflow
					child.onError(e.getCause());
				} else {
					child.onError(e);
				}
			} catch (Throwable t) {
				child.onError(t);
			}
		}

		private void drainNow() {
			// get the number of unsatisfied requests
			long requests = get();
			while (true) {
				// reset drainRequested counter
				drainRequested.set(1);
				long emitted = 0;
				while (requests > 0) {
					if (child.isUnsubscribed()) {
						// leave drainRequested > 0 to prevent more
						// scheduling of drains
						return;
					} else {
						T item = queue.poll();
						if (item == null) {
							// queue is empty
							if (finished(true)) {
								return;
							} else {
								// another drain was requested so go
								// round again but break out of this
								// while loop to the outer loop so we
								// can update requests and reset drainRequested
								break;
							}
						} else {
							// there was an item on the queue
							child.onNext(item);
							requests--;
							emitted++;
						}
					}
				}
				requests = addAndGet(-emitted);
				if (requests == 0L && finished(queue.isEmpty())) {
					return;
				}
			}
		}

		private boolean finished(boolean isQueueEmpty) {
			if (done) {
				// assign volatile to a temp variable so we don't
				// read it more than once
				Throwable t = error;
				if (isQueueEmpty) {
					try {
						// first close the queue (which in this case though
						// empty also disposes of its resources)
						queue.close();

						if (t != null) {
							child.onError(t);
						} else {
							child.onCompleted();
						}
						// leave drainRequested > 0 so that further drain
						// requests are ignored
						return true;
					} finally {
						worker.unsubscribe();
					}

				} else if (t != null && !delayError) {
					try {
						// queue is not empty but we are going to shortcut
						// that because delayError is false

						// first close the queue (which in this case also
						// disposes of its resources)
						queue.close();

						// now report the error
						child.onError(t);

						// leave drainRequested > 0 so that further drain
						// requests are ignored
						return true;
					} finally {
						worker.unsubscribe();
					}
				} else {
					// otherwise we need to wait for all items waiting
					// on the queue to be requested and delivered
					// (delayError=true)
					return drainRequested.compareAndSet(1, 0);
				}
			} else {
				return drainRequested.compareAndSet(1, 0);
			}
		}
	}

	private static <T> Subscription disposer(final CloseableQueue<T> queue) {
		return new Subscription() {

			private volatile boolean isUnsubscribed = false;

			@Override
			public void unsubscribe() {
				isUnsubscribed = true;
				try {
					// note that db is configured to attempt to delete files
					// after close
					queue.close();
				} catch (Throwable e) {
					// there is no facility to report unsubscription
					// failures in
					// the observable chain so write to stderr
					e.printStackTrace();
				}
			}

			@Override
			public boolean isUnsubscribed() {
				return isUnsubscribed;
			}
		};
	}

	private static final class MapDbSerializer<T> implements Serializer<T>, Serializable {

		private static final long serialVersionUID = -4992031045087289671L;
		private transient final DataSerializer<T> dataSerializer;

		MapDbSerializer(DataSerializer<T> dataSerializer) {
			this.dataSerializer = dataSerializer;
		}

		@Override
		public T deserialize(final DataInput input, int availableBytes) throws IOException {
			return dataSerializer.deserialize(input, availableBytes);
		}

		@Override
		public int fixedSize() {
			return -1;
		}

		@Override
		public void serialize(DataOutput output, T t) throws IOException {
			dataSerializer.serialize(output, t);
		}
	};

}
