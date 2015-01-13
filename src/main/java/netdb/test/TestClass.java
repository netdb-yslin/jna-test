package netdb.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import netdb.test.NativeIO;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class TestClass {

	private static int ITER_COUNT = 10000;
	private static int RUN_PER_ITER = 500;
	private static int THREAD_COUNT = 10;

	public static void main(String[] args) throws InterruptedException {

		// Get arguments
		if (args.length > 0)
			ITER_COUNT = Integer.parseInt(args[0]);
		if (args.length > 1)
			RUN_PER_ITER = Integer.parseInt(args[1]);
		if (args.length > 2)
			THREAD_COUNT = Integer.parseInt(args[2]);

		// Allocate Memory
		PointerByReference memoryPointer = new PointerByReference();
		int error = NativeIO.posix_memalign(memoryPointer,
				new NativeLong(4096), new NativeLong(1024 * 1024));
		if (error != 0) {
			System.out.println("Error: can't allocate memory with error code: "
					+ error);
			return;
		}

		// start execution
		Executor executor = Executors.newFixedThreadPool(THREAD_COUNT + 100);
		runConcurrencyTasks(executor, THREAD_COUNT, memoryPointer.getValue());
	}

	public static void runConcurrencyTasks(Executor executor, int concurrency, final Pointer pointer)
			throws InterruptedException {

		final CountDownLatch ready = new CountDownLatch(concurrency);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(concurrency);

		for (int i = 0; i < concurrency; i++) {
			executor.execute(new Runnable() {
				public void run() {
					Task task = new Task(pointer);
					
					ready.countDown(); // Tell main thread we're ready
					try {
						start.await(); // Wait till all other threads are ready
						
						// Run Tests
						for (int itrNum = ITER_COUNT; itrNum > 0; itrNum--)
							task.run();
						
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						
						// Output results
						StringBuilder sb = new StringBuilder();
						sb.append("Max Time: " + task.getMax() / 1000 + " us\t");
						sb.append("Average Time: " + (task.getAverage() / 1000) + " us");
						System.out.println(sb.toString());
						
						done.countDown(); // Tell timer we're done
					}
				}
			});
		}

		ready.await(); // Wait for all workers to be ready
		start.countDown(); // And they're off!
		done.await();
	}

	static class Task {
		private Pointer pointer;
		private long numOfRun = 0;
		private int sum = 0; // in nanoseconds
		private long max = 0; // in nanoseconds

		public Task(Pointer pointer) {
			this.pointer = pointer;
		}

		public void run() {
			byte[] buffer = new byte[32];
			long startNanos = System.nanoTime();
			
			for (int i = 0; i < RUN_PER_ITER; i++)
				pointer.read(0, buffer, 0, buffer.length);

			long time = System.nanoTime() - startNanos;
			numOfRun++;
			sum += time;
			if (time > max)
				max = time;
		}

		public long getMax() {
			return max;
		}

		public long getAverage() {
			return (long) (sum / numOfRun);
		}
	}
}
