package org.agrinext.agrimobile.Activities

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.mntechnique.otpmobileauth.auth.AuthReqCallback
import kotlinx.android.synthetic.main.activity_user_profile.*
import org.agrinext.agrimobile.Android.FrappeClient
import org.agrinext.agrimobile.R
import org.json.JSONObject
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import org.agrinext.agrimobile.Android.PermissionUtils
import android.provider.MediaStore
import android.content.Intent
import android.app.Activity
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.support.v4.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*


class UserProfile : Fragment() {
    var frappeClient: FrappeClient? = null
    var userChoosenTask:String? = null

    companion object {
        val SELECT_FILE = 0
        val REQUEST_CAMERA = 1
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        frappeClient = FrappeClient(context)
        return inflater?.inflate(R.layout.activity_user_profile, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfileText()
        setupProfilePhoto()
    }

    fun setupProfilePhoto() {
        ivProfileImage.setOnClickListener {
            selectImage()
        }
    }

    fun setupProfileText() {
        val request = OAuthRequest(Verb.GET, frappeClient?.getServerURL() + getString(R.string.openIDEndpoint))
        val callback = object : AuthReqCallback{
            override fun onErrorResponse(error: String) {
                Log.d("responseError", error)
            }

            override fun onSuccessResponse(result: String) {
                val jsonResponse = JSONObject(result)
                tvFullName.setText(jsonResponse.getString("name"))
            }
        }
        frappeClient?.executeRequest(request, callback)
    }

    fun selectImage() {
        val items = arrayOf<CharSequence>("Take Photo", "Choose from Library", "Cancel")
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Add Photo!")
        builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
            val result = PermissionUtils().checkStoragePermission(activity)
            if (items[item] == "Take Photo") {
                userChoosenTask = "Take Photo"
                if (result) {
                    val cameraPerm = PermissionUtils().checkCameraPermission(activity)
                    if (cameraPerm) cameraIntent()
                }
            } else if (items[item] == "Choose from Library") {
                userChoosenTask = "Choose from Library"
                if (result)
                    galleryIntent()
            } else if (items[item] == "Cancel") {
                dialog.dismiss()
            }
        })
        builder.show()
    }

    fun cameraIntent() {

        val mIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(mIntent, REQUEST_CAMERA)
        /*

        val mIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        mIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (mIntent.resolveActivity(activity.packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.i(TAG, "IOException")
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                FileProvider.getUriForFile(context, context.applicationContext.packageName + ".FrappeFileProvider", createImageFile());
                activity.startActivityForResult(mIntent, REQUEST_CAMERA)
            }
        }

        */
    }

    fun galleryIntent() {
        val mIntent = Intent()
        mIntent.type = "image/*"
        mIntent.action = Intent.ACTION_GET_CONTENT//
        activity.startActivityForResult(Intent.createChooser(mIntent, "Select File"), SELECT_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data!!)
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data!!)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun onSelectFromGalleryResult(data: Intent?) {
        if (data != null) {
            val bm = MediaStore.Images.Media.getBitmap(activity.applicationContext.getContentResolver(), data.data)
            ivProfileImage.setImageBitmap(bm)
        }
    }

    fun onCaptureImageResult(data:Intent) {
        val thumbnail = data.extras.get("data") as Bitmap
        val bytes = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val destination = File(getExternalStorageDirectory(),
                System.currentTimeMillis().toString() + ".jpg")
        val fo: FileOutputStream
        destination.createNewFile()
        fo = FileOutputStream(destination)
        fo.write(bytes.toByteArray())
        fo.close()
        ivProfileImage.setImageBitmap(thumbnail)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, // prefix
                ".jpg", // suffix
                storageDir      // directory
        )

        // Save a file: path for use with ACTION_VIEW intents
        // mCurrentPhotoPath = "file:" + image.absolutePath
        return image
    }
}
