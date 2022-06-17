package com.example.the_memory_game

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.service.autofill.VisibilitySetterAction
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.the_memory_game.models.BoardSize
import com.example.the_memory_game.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object{
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val TAG ="CreateActivity"
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private lateinit var adapter:ImagePickerApadter
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired= boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0/$numImagesRequired)"

        btnSave.setOnClickListener{
            saveDataToFirebase()
        }
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
              btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {}

        })

         adapter = ImagePickerApadter(this,chosenImageUris,boardSize,object: ImagePickerApadter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    launchIntentforPhotos()
                } else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode== READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentforPhotos()
            }
            else{
                Toast.makeText(this,"In order to create a custom game, app needs permissions",Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from launched activity, user likely cancelled flow")
            return
        }
        val selectedUri = data.data         //only able to select single photo
        val clipData = data.clipData          //able to select multiple photos
        if (clipData != null) {
            Log.i(TAG, "clipdata numImages ${clipData.itemCount} : $clipData ")
            for (i in 0 until clipData.itemCount) {
                val clipitem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipitem.uri)
                }
            }
        }
        else if(selectedUri!=null){
            Log.i(TAG,"data: ${selectedUri}")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} /$numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
        
    }

    private fun shouldEnableSaveButton(): Boolean {
         //enable save button or not
        if(chosenImageUris.size != numImagesRequired){
            return false
        }
        if(etGameName.text.isBlank()|| etGameName.text.length< MIN_GAME_NAME_LENGTH)
        {
            return false
        }
        return true
    }

    private fun launchIntentforPhotos() {
    val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent,"Choose pictures"),PICK_PHOTO_CODE)
    }
    private fun saveDataToFirebase() {
        Log.i(TAG,"save data to firebase")
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name Taken")
                    .setMessage("A game name already exists with the name $customGameName, Please choose another")
                    .setPositiveButton("Ok", null)
                    .show()
                btnSave.isEnabled= true
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener{exception ->
            Log.e(TAG,"Encountered error while saving memory game", exception)
            Toast.makeText(this,"Couldn't save the game, error occured",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled=true
        }
    }


    private fun handleImageUploading(gameName: String) {
        Log.i(TAG,"save photos to database")
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageURL = mutableListOf<String>()
        for((index,photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImagebyteArray(photoUri)
            val filepath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filepath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG,"Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener{ downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful){
                        Log.e(TAG,"Exception with firebase storage", downloadUrlTask.exception)
                        Toast.makeText(this,"Failed to upload image",Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError){
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageURL.add(downloadUrl)
                    pbUploading.progress = uploadedImageURL.size * 100/chosenImageUris.size
                    Log.i(TAG,"finished uploading $photoUri, num uploaded ${uploadedImageURL.size}")
                    if(uploadedImageURL.size == chosenImageUris.size){
                        handleAllImagesUploaded(gameName,uploadedImageURL)
                    }
                }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener{ gameCreationTask ->
                pbUploading.visibility = View.GONE
                if(!gameCreationTask.isSuccessful){
                    Log.e(TAG,"Exception with game creation",gameCreationTask.exception)
                    Toast.makeText(this,"Failed game creation",Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
               Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! lets play your game $gameName")
                    .setPositiveButton("ok"){_,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME,gameName)
                        setResult(Activity.RESULT_OK,resultData)
                        finish()
                    }.show()
            }

    }

    private fun getImagebyteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "original width ${originalBitmap.width} and height ${originalBitmap.height}")

        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
   val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()
    }
}