/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.view.activity

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.toshi.BuildConfig
import com.toshi.R
import com.toshi.exception.PermissionException
import com.toshi.extensions.startActivity
import com.toshi.extensions.toast
import com.toshi.model.local.User
import com.toshi.model.network.UserDetails
import com.toshi.util.FileUtil
import com.toshi.util.ImageUtil
import com.toshi.util.OnSingleClickListener
import com.toshi.util.PermissionUtil
import com.toshi.view.fragment.dialogFragment.ChooserDialog
import com.toshi.viewModel.EditProfileViewModel
import kotlinx.android.synthetic.main.activity_edit_profile.avatar
import kotlinx.android.synthetic.main.activity_edit_profile.closeButton
import kotlinx.android.synthetic.main.activity_edit_profile.editProfilePhoto
import kotlinx.android.synthetic.main.activity_edit_profile.inputAbout
import kotlinx.android.synthetic.main.activity_edit_profile.inputLocation
import kotlinx.android.synthetic.main.activity_edit_profile.inputName
import kotlinx.android.synthetic.main.activity_edit_profile.inputUsername
import kotlinx.android.synthetic.main.activity_edit_profile.publicSwitch
import kotlinx.android.synthetic.main.activity_edit_profile.saveButton
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    companion object {
        private const val PICK_IMAGE = 1
        private const val CAPTURE_IMAGE = 2
        private const val INTENT_TYPE = "image/*"
    }

    private lateinit var viewModel: EditProfileViewModel

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.activity_edit_profile)
        init()
    }

    private fun init() {
        initViewModel()
        initClickListeners()
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(EditProfileViewModel::class.java)
    }

    private fun initClickListeners() {
        closeButton.setOnClickListener { finish() }
        avatar.setOnClickListener { handleAvatarClicked() }
        editProfilePhoto.setOnClickListener { handleAvatarClicked() }
        saveButton.setOnClickListener(saveClickListener)
    }

    private val saveClickListener = object : OnSingleClickListener() {
        override fun onSingleClick(v: View?) {
            val userDetails = UserDetails(
                    name = inputName.text.toString().trim(),
                    username = inputUsername.text.toString().trim(),
                    description = inputAbout.text.toString().trim(),
                    location = inputLocation.text.toString().trim(),
                    public = publicSwitch.isChecked
            )

            viewModel.updateUser(userDetails)
        }
    }

    private fun handleAvatarClicked() {
        val chooserDialog = ChooserDialog.newInstance()
        chooserDialog.setOnChooserClickListener(object : ChooserDialog.OnChooserClickListener {
            override fun captureImageClicked() = checkCameraPermission()
            override fun importImageFromGalleryClicked() = checkExternalStoragePermission()
        })
        chooserDialog.show(supportFragmentManager, ChooserDialog.TAG)
    }

    private fun checkExternalStoragePermission() {
        PermissionUtil.hasPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                PermissionUtil.READ_EXTERNAL_STORAGE_PERMISSION,
                { startGalleryActivity() }
        )
    }

    private fun checkCameraPermission() {
        PermissionUtil.hasPermission(
                this,
                Manifest.permission.CAMERA,
                PermissionUtil.CAMERA_PERMISSION,
                { startCameraActivity() }
        )
    }

    private fun initObservers() {
        viewModel.user.observe(this, Observer {
            user -> user?.let { handleUser(it) }
        })
        viewModel.userUpdated.observe(this, Observer {
            toast(R.string.user_updated); finish() }
        )
        viewModel.displayNameError.observe(this, Observer {
            displayNameError -> displayNameError?.let { handleDisplayNameError(it) }
        })
        viewModel.usernameError.observe(this, Observer {
            usernameError -> usernameError?.let { handleUsernameError(it) }
        })
        viewModel.error.observe(this, Observer {
            error -> error?.let { toast(it) }
        })
    }

    private fun handleUser(user: User) {
        setUserFields(user)
        loadAvatar(user.avatar)
    }

    private fun setUserFields(user: User) {
        user.displayName?.let { inputName.setText(it) }
        user.usernameForEditing?.let { inputUsername.setText(it) }
        user.about?.let { inputAbout.setText(it) }
        user.location?.let { inputLocation.setText(it) }
        user.isPublic.let { publicSwitch.isChecked = it }
    }

    private fun loadAvatar(avatarUrl: String?) {
        avatarUrl?.let {
            avatar.setImageResource(0)
            ImageUtil.loadFromNetwork(avatarUrl, avatar)
        } ?: run {
            avatar.setImageDrawable(null)
            avatar.setImageResource(R.color.textColorHint)
        }
    }

    private fun handleDisplayNameError(errorMessage: Int) {
        inputName.error = getString(errorMessage)
        inputName.requestFocus()
    }

    private fun handleUsernameError(errorMessage: Int) {
        inputUsername.error = getString(errorMessage)
        inputUsername.requestFocus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!PermissionUtil.isPermissionGranted(grantResults)) return

        when (requestCode) {
            PermissionUtil.CAMERA_PERMISSION -> startCameraActivity()
            PermissionUtil.READ_EXTERNAL_STORAGE_PERMISSION -> startGalleryActivity()
            else -> throw PermissionException("This permission doesn't belong in this context")
        }
    }

    private fun startCameraActivity() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) == null) return
        val photoFile = FileUtil.createImageFileWithRandomName()
        viewModel.capturedImagePath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + FileUtil.FILE_PROVIDER_NAME,
                photoFile
        )
        PermissionUtil.grantUriPermission(this, cameraIntent, photoURI)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(cameraIntent, CAPTURE_IMAGE)
    }

    private fun startGalleryActivity() {
        val pickPictureIntent = Intent()
                .setType(INTENT_TYPE)
                .setAction(Intent.ACTION_GET_CONTENT)
        if (pickPictureIntent.resolveActivity(packageManager) == null) return
        val chooserIntent = Intent.createChooser(
                pickPictureIntent,
                getString(R.string.select_picture)
        )
        startActivityForResult(chooserIntent, PICK_IMAGE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultIntent)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            PICK_IMAGE -> startImageCropActivity(resultIntent?.data)
            CAPTURE_IMAGE -> startImageCropActivity(viewModel.capturedImagePath)
        }
    }

    private fun startImageCropActivity(capturedImagePath: String?) {
        capturedImagePath?.let {
            val imageUri = Uri.fromFile(File(it))
            startImageCropActivity(imageUri)
        }
    }

    private fun startImageCropActivity(imageUri: Uri?) = imageUri?.let {
        startActivity<ImageCropActivity> { putExtra(ImageCropActivity.IMAGE_URI, imageUri) }
    }
}