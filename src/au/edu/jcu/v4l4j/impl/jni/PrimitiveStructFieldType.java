package au.edu.jcu.v4l4j.impl.jni;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public enum PrimitiveStructFieldType implements StructFieldType {
	/**
	 * bool type
	 */
	BOOL ((buffer, value) -> {
			boolean bValue;
			if (value == null)
				bValue = false;
			else if (value instanceof Boolean)
				bValue = (Boolean) value;
			else
				bValue = Boolean.valueOf(value.toString());
			buffer.put((byte) (bValue ? 0xFF : 0x00));
		},
		(buffer, params) -> (buffer.get() != 0)),
	INT8(
			(buffer, value) -> buffer.put(((Number)value).byteValue()),
			(buffer, context) -> ((Number) buffer.get())),
	INT16(
			(buffer, value) -> buffer.putShort(((Number)value).shortValue()),
			(buffer, context) -> buffer.getShort()),
	INT32(
			(buffer, value) -> buffer.putInt(((Number)value).intValue()),
			(buffer, context) -> buffer.getInt()),
	INT64(
			(buffer, value) -> buffer.putLong(((Number)value).longValue()),
			(buffer, context) -> buffer.getLong()),
	FLOAT32(
			(buffer, value) -> buffer.putFloat(((Number)value).floatValue()),
			(buffer, context) -> buffer.getFloat()),
	FLOAT64(
			(buffer, value) -> buffer.putDouble(((Number)value).doubleValue()),
			(buffer, context) -> buffer.getDouble()),
	RAW_POINTER(
			(buffer, value) -> {
				if (PrimitiveStructFieldType.values()[7].getSize() == 4)
					buffer.putInt(((Number)value).intValue());
				else
					buffer.putLong(((Number)value).longValue());
			},
			(buffer, context) -> {
				if (PrimitiveStructFieldType.values()[7].getSize() == 4)
					return (long) buffer.getInt();
				else
					return buffer.getLong();
			}),
	;
	private final int alignment;
	private final int size;
	private final BiConsumer<ByteBuffer, Object> writer;
	private final BiFunction<ByteBuffer, StructReadingContext, Object> reader;
	
	PrimitiveStructFieldType(BiConsumer<ByteBuffer, Object> writer, BiFunction<ByteBuffer, StructReadingContext, Object> reader) {
		this.writer = writer;
		this.reader = reader;
		this.alignment = MemoryUtils.getAlignment(this.ordinal());
		this.size = MemoryUtils.getSize(this.ordinal());
	}
	
	@Override
	public int getAlignment() {
		return alignment;
	}
	
	@Override
	public int getSize() {
		return size;
	}

	@Override
	public boolean expands() {
		return false;
	}
	
	@Override
	public void write(ByteBuffer buffer, Object params) {
		writer.accept(buffer, params);
	}
	
	@Override
	public Object read(ByteBuffer buffer, StructReadingContext context) {
		return reader.apply(buffer, context);
	}
}