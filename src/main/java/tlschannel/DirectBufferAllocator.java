package tlschannel;

import java.nio.ByteBuffer;

/**
 * Allocator that creates direct buffers. The {@link #free(ByteBuffer)} method,
 * if called, deallocates the buffer immediately, without having to wait for GC
 * (and the finalizer) to run. Calling {@link #free(ByteBuffer)} is actually
 * optional, but should result in reduced memory consumption.
 *
 * Direct buffers are generally preferred for using with I/O, to avoid an extra
 * user-space copy, or to reduce garbage collection overhead.
 */
public class DirectBufferAllocator implements BufferAllocator {

    @Override
    public ByteBuffer allocate(final int size) {
        return ByteBuffer.allocateDirect(size);
    }

    @Override
    public void free(final ByteBuffer buffer) {
        // do not wait for GC (and finalizer) to run
    }

}
