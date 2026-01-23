package com.example.client.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * 1. Hàm chuyển đổi Uri (content://...) thành File thực tế trong bộ nhớ tạm.
     * Dùng để gửi file ảnh lên Server qua MultipartBody.
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            // Tạo file tạm thời trong cache của ứng dụng
            val tempFile = File.createTempFile("upload_avatar", ".jpg", context.cacheDir)
            tempFile.deleteOnExit() // Tự động xóa khi app tắt để đỡ rác máy

            val outputStream = FileOutputStream(tempFile)

            // Copy dữ liệu từ Uri sang File tạm
            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 2. Hàm chuyển đổi chuỗi Base64 thành Bitmap để hiển thị lên ảnh (Code cũ của bạn).
     * Dùng khi server trả về chuỗi base64 thay vì link ảnh.
     */
    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            // Cắt bỏ phần header "data:image/jpeg;base64," nếu server gửi kèm
            val pureBase64 = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }

            // Giải mã chuỗi Base64 thành mảng byte
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)

            // Chuyển mảng byte thành Bitmap
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}