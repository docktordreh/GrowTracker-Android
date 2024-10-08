package me.anon.lib.helper

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.anon.lib.adapter.ActionJsonAdapter
import me.anon.lib.adapter.ArrayListJsonAdapter
import me.anon.model.Action
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type

object MoshiHelper
{
	@JvmStatic
	public fun <T> parse(json: String, type: Type): T
	{
		return getMoshi().adapter<T>(type).fromJson(json) as T
	}

	@JvmStatic
	public fun <T> parse(json: File, type: Type): T
	{
		return getMoshi().adapter<T>(type).fromJson(JsonReader.of(json.source().buffer())) as T
	}

	@JvmStatic
	public fun <T> parse(json: InputStream, type: Type): T
	{
		return getMoshi().adapter<T>(type).fromJson(JsonReader.of(json.source().buffer())) as T
	}

	@JvmStatic
	public fun toJson(obj: Any): String
	{
		return getMoshi().adapter(obj.javaClass).toJson(obj)
	}

	@JvmStatic
	public fun <T> toJson(obj: T, type: Type): String
	{
		return getMoshi().adapter<T>(type).toJson(obj)
	}

	@JvmStatic
	public fun <T> toJson(obj: T, type: Type, outputStream: OutputStream)
	{
		getMoshi().adapter<T>(type).toJson(JsonWriter.of(outputStream.sink().buffer()), obj)
	}

	public fun getMoshi(): Moshi
	{
		val moshi = Moshi.Builder()
		moshi.add<Action>(Action::class.java, ActionJsonAdapter())
		addAdapters(moshi)

		return moshi.build()
	}

	public fun addAdapters(builder: Moshi.Builder): Moshi.Builder
	{
		builder.add(ArrayListJsonAdapter.FACTORY)
		builder.add(KotlinJsonAdapterFactory())
		return builder
	}
}
