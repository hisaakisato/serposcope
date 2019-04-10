package com.serphacker.serposcope.util;

public interface ThrowableConsumer<T> {
	void accept(T t) throws Exception;
}
