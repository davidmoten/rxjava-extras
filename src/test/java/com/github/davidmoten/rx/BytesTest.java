package com.github.davidmoten.rx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rx.util.ZippedEntry;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class BytesTest {

	@Test
	public void testUnzip() {
		List<String> list = Bytes.unzip(new File("src/test/resources/test.zip"))
				.concatMap(new Func1<ZippedEntry, Observable<String>>() {

					@Override
					public Observable<String> call(ZippedEntry entry) {
						return Observable.just(entry.getName()).concatWith(Strings.from(entry.getInputStream()));
					}
				}).toList().toBlocking().single();
		assertEquals(Arrays.asList("document1.txt", "hello there", "document2.txt", "how are you going?"), list);
	}

	@Test
	public void testUnzipPartial() {
		InputStream is = BytesTest.class.getResourceAsStream("/test.zip");
		assertNotNull(is);
		List<String> list = Bytes.unzip(is).concatMap(new Func1<ZippedEntry, Observable<String>>() {

			@Override
			public Observable<String> call(ZippedEntry entry) {
				try {
					return Observable.just((char) entry.getInputStream().read() + "");
				} catch (IOException e) {
					return Observable.error(e);
				}
			}
		}).toList().toBlocking().single();
		assertEquals(Arrays.asList("h", "h"), list);
	}

	@Test
	public void testUnzipExtractSpecificFile() {
		List<String> list = Bytes.unzip(new File("src/test/resources/test.zip"))
				.filter(new Func1<ZippedEntry, Boolean>() {

					@Override
					public Boolean call(ZippedEntry entry) {
						return entry.getName().equals("document2.txt");
					}
				}).concatMap(new Func1<ZippedEntry, Observable<String>>() {

					@Override
					public Observable<String> call(ZippedEntry entry) {
						return Strings.from(entry.getInputStream());
					}
				}).toList().toBlocking().single();
		assertEquals(Arrays.asList("how are you going?"), list);
	}

	@Test
	public void isUtilClass() {
		Asserts.assertIsUtilityClass(Bytes.class);
	}

	@Test
	public void testBytesFromFile() throws IOException {
		File file = new File("target/testFromFile");
		file.delete();
		FileOutputStream out = new FileOutputStream(file);
		out.write("abcdefg".getBytes());
		out.close();
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Bytes //
				.from(file, 4) //
				.doOnNext(new Action1<byte[]>() {
					@Override
					public void call(byte[] b) {
						try {
							bytes.write(b);
						} catch (IOException e) {
							throw new RuntimeException();
						}
					}
				}).subscribe();
		bytes.close();
		assertArrayEquals("abcdefg".getBytes(), bytes.toByteArray());
	}

}
