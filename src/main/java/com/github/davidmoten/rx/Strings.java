package com.github.davidmoten.rx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.davidmoten.rx.internal.operators.OnSubscribeReader;
import com.github.davidmoten.util.Preconditions;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;

public final class Strings {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	/**
	 * Returns null if input is null otherwise returns input.toString().trim().
	 */
	private static Func1<Object, String> TRIM = new Func1<Object, String>() {

		@Override
		public String call(Object input) {
			if (input == null)
				return null;
			else
				return input.toString().trim();
		}
	};

	@SuppressWarnings("unchecked")
	public static <T> Func1<T, String> trim() {
		return (Func1<T, String>) TRIM;
	}

	public static Observable<String> from(Reader reader, int bufferSize) {
		return Observable.create(new OnSubscribeReader(reader, bufferSize));
	}

	public static Observable<String> from(Reader reader) {
		return from(reader, 8192);
	}

	public static Observable<String> from(InputStream is) {
		return from(new InputStreamReader(is));
	}

	public static Observable<String> from(InputStream is, Charset charset) {
		return from(new InputStreamReader(is, charset));
	}

	public static Observable<String> from(InputStream is, Charset charset, int bufferSize) {
		return from(new InputStreamReader(is, charset), bufferSize);
	}

	public static Observable<String> split(Observable<String> source, String pattern) {
		return source.compose(Transformers.split(pattern));
	}

	public static Observable<String> concat(Observable<String> source) {
		return join(source, "");
	}

	public static Observable<String> concat(Observable<String> source, final String delimiter) {
		return join(source, delimiter);
	}

	public static Observable<String> strings(Observable<?> source) {
		return source.map(new Func1<Object, String>() {
			@Override
			public String call(Object t) {
				return String.valueOf(t);
			}
		});
	}

	public static Observable<String> from(File file) {
		return from(file, DEFAULT_CHARSET);
	}

	public static Observable<String> from(final File file, final Charset charset) {
		Preconditions.checkNotNull(file);
		Preconditions.checkNotNull(charset);
		Func0<Reader> resourceFactory = new Func0<Reader>() {
			@Override
			public Reader call() {
				try {
					return new InputStreamReader(new FileInputStream(file), charset);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		};
		return from(resourceFactory);
	}

	public static Observable<String> fromClasspath(final String resource, final Charset charset) {
		Preconditions.checkNotNull(resource);
		Preconditions.checkNotNull(charset);
		Func0<Reader> resourceFactory = new Func0<Reader>() {
			@Override
			public Reader call() {
				return new InputStreamReader(Strings.class.getResourceAsStream(resource), charset);
			}
		};
		return from(resourceFactory);
	}
	
	public static Observable<String> fromClasspath(final String resource) {
		return fromClasspath(resource, Utf8Holder.INSTANCE);
	}
	
	private static class Utf8Holder {
		static final Charset INSTANCE = Charset.forName("UTF-8");
	}

	public static Observable<String> from(final Func0<Reader> readerFactory) {
		Func1<Reader, Observable<String>> observableFactory = new Func1<Reader, Observable<String>>() {
			@Override
			public Observable<String> call(Reader reader) {
				return from(reader);
			}
		};
		return Observable.using(readerFactory, observableFactory, DisposeActionHolder.INSTANCE, true);
	}
	
	private static class DisposeActionHolder {
		static final Action1<Reader> INSTANCE = new Action1<Reader>() {
			@Override
			public void call(Reader reader) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}

	public static Observable<String> join(Observable<String> source) {
		return join(source, "");
	}

	public static Observable<String> decode(Observable<byte[]> source, CharsetDecoder decoder) {
		return source.compose(Transformers.decode(decoder));
	}

	public static Observable<String> decode(Observable<byte[]> source, Charset charset) {
		return decode(source, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE));
	}

	public static Observable<String> decode(Observable<byte[]> source, String charset) {
		return decode(source, Charset.forName(charset));
	}

	public static Observable<String> join(final Observable<String> source, final String delimiter) {

		return Observable.defer(new Func0<Observable<String>>() {
			final AtomicBoolean afterFirst = new AtomicBoolean(false);
			final AtomicBoolean isEmpty = new AtomicBoolean(true);

			@Override
			public Observable<String> call() {
				return source.collect(new Func0<StringBuilder>() {
					@Override
					public StringBuilder call() {
						return new StringBuilder();
					}
				}, new Action2<StringBuilder, String>() {

					@Override
					public void call(StringBuilder b, String s) {
						if (!afterFirst.compareAndSet(false, true)) {
							b.append(delimiter);
						}
						b.append(s);
						isEmpty.set(false);
					}
				}).flatMap(new Func1<StringBuilder, Observable<String>>() {

					@Override
					public Observable<String> call(StringBuilder b) {
						if (isEmpty.get())
							return Observable.empty();
						else
							return Observable.just(b.toString());
					}
				});

			}
		});
	}

}
