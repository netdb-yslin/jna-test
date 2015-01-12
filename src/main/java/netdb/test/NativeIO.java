package netdb.test;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.ptr.PointerByReference;

public class NativeIO {

	private static boolean inited = false;

	static {
		try {
			// Check platform
			if (Platform.isLinux()) {
				inited = true;
				Native.register(Platform.C_LIBRARY_NAME); // get access to posix_memalign(...)
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static native int posix_memalign(PointerByReference memptr,
			NativeLong alignment, NativeLong size);
}
