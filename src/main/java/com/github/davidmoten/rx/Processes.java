package com.github.davidmoten.rx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.github.davidmoten.util.Optional;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public final class Processes {

    public static void main(String[] args) throws IOException, InterruptedException {
        execute("ls").map(new Func1<byte[], String>() {

            @Override
            public String call(byte[] bytes) {
                return new String(bytes);
            }
        });
    }

    public static Observable<byte[]> execute(String... command) {
        return execute(
                new Parameters(Arrays.asList(command), Optional.<Map<String, String>> absent(),
                        true, new File("."), Optional.<Long> absent()));
    }

    public static Observable<byte[]> execute(final Parameters parameters) {
        Func0<Process> resourceFactory = new Func0<Process>() {

            @Override
            public Process call() {
                ProcessBuilder b = new ProcessBuilder(parameters.command());
                if (parameters.env().isPresent()) {
                    if (parameters.appendEnv())
                        b.environment().clear();
                    b.environment().putAll(parameters.env().get());
                }
                b.directory(parameters.directory());
                b.redirectErrorStream(true);
                try {
                    return b.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Func1<Process, Observable<byte[]>> factory = new Func1<Process, Observable<byte[]>>() {
            @Override
            public Observable<byte[]> call(final Process process) {
                InputStream is = process.getInputStream();
                Observable<byte[]> output;
                if (is != null)
                    output = Bytes.from(is);
                else
                    output = Observable.empty();
                Observable<byte[]> completion = Observable.create(new OnSubscribe<byte[]>() {
                    @Override
                    public void call(Subscriber<? super byte[]> sub) {
                        try {
                            // TODO waitFor does not exist pre 1.8 with timeout!
                            // parameters.waitForMs().get(),TimeUnit.MILLISECONDS);

                            if (parameters.waitForMs().isPresent()) {
                                // boolean finished = process.waitFor(
                                // parameters.waitForMs().get(),
                                // TimeUnit.MILLISECONDS);
                                // if (!finished) {
                                // sub.onError(new TimeoutException("process
                                // timed out"));
                                // return;
                                // }
                                sub.onError(new IllegalArgumentException("not implemented yet"));
                            } else {
                                int exitCode = process.waitFor();
                                if (exitCode != 0)
                                    sub.onError(new ProcessException(exitCode));
                                return;
                            }
                            sub.onCompleted();
                        } catch (InterruptedException e) {
                            sub.onError(e);
                        }
                    }
                }).subscribeOn(Schedulers.io());
                return output.concatWith(completion);
            }
        };
        Action1<? super Process> disposeAction = new Action1<Process>() {
            @Override
            public void call(Process process) {
                process.destroy();
            }
        };
        return Observable.using(resourceFactory, factory, disposeAction);
    }

    public static class ProcessException extends RuntimeException {
        private static final long serialVersionUID = 722422557667123473L;

        private final int exitCode;

        public ProcessException(int exitCode) {
            super("process returned exitCode " + exitCode);
            this.exitCode = exitCode;
        }

        public int exitCode() {
            return exitCode;
        }

    }

    public static final class Parameters {
        private final List<String> command;
        private final Optional<Map<String, String>> env;
        private final boolean appendEnv;
        private final File directory;
        private final Optional<Long> waitForMs;

        public Parameters(List<String> command, Optional<Map<String, String>> env,
                boolean appendEnv, File directory, Optional<Long> waitForMs) {
            this.command = command;
            this.env = env;
            this.appendEnv = appendEnv;
            this.directory = directory;
            this.waitForMs = waitForMs;
        }

        public Optional<Long> waitForMs() {
            return waitForMs;
        }

        public File directory() {
            return directory;
        }

        public List<String> command() {
            return command;
        }

        public Optional<Map<String, String>> env() {
            return env;
        }

        public boolean appendEnv() {
            return appendEnv;
        }

    }
}
