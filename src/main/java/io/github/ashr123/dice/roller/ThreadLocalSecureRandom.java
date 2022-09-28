package io.github.ashr123.dice.roller;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ThreadLocalSecureRandom
{
	private static final ThreadLocal<SecureRandom> SECURE_RANDOM_THREAD_LOCAL = new ThreadLocal<>();

	private ThreadLocalSecureRandom()
	{
	}

	public static SecureRandom current() throws NoSuchAlgorithmException
	{
		SecureRandom secureRandom = SECURE_RANDOM_THREAD_LOCAL.get();
		if (secureRandom == null)
			SECURE_RANDOM_THREAD_LOCAL.set(secureRandom = SecureRandom.getInstanceStrong());
		return secureRandom;
	}
}