package com.github.davidmoten.rx.exceptions;

import java.io.IOException;

public class IORuntimeException extends RuntimeException{

	private static final long serialVersionUID = 9073088105890631472L;

	public IORuntimeException(IOException e) {
		super(e);
	}
	
}
