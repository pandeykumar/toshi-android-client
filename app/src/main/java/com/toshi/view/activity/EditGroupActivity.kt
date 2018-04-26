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
import com.toshi.extensions.isVisible
import com.toshi.extensions.toast
import com.toshi.model.local.Group
import com.toshi.util.FileUtil
import com.toshi.util.ImageUtil
import com.toshi.util.OnSingleClickListener
import com.toshi.util.PermissionUtil
import com.toshi.view.fragment.dialogFragment.ChooserDialog
import com.toshi.viewModel.EditGroupViewModel
import com.toshi.viewModel.ViewModelFactory.EditGroupViewModelFactory
import kotlinx.android.synthetic.main.activity_edit_group.*
import java.io.File

class EditGroupActivity : AppCompatActivity() {

    companion object {
        private val PICK_IMAGE = 1
        private val CAPTURE_IMAGE = 2
        private val INTENT_TYPE = "image/*"
        const val EXTRA__GROUP_ID = "extra_group_id"
    }

    private lateinit var viewModel: EditGroupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_group)
        init()
    }

    private fun init() {
        initViewModel()
        fetchGroup()
        initClickListeners()
        initObservers()
    }

    private fun initViewModel() {
        val groupId = getGroupIdFromIntent()
        if (groupId == null) {
            toast(R.string.invalid_group)
            finish()
            return
        }

        viewModel = ViewModelProviders.of(
                this,
                EditGroupViewModelFactory(groupId)
        ).get(EditGroupViewModel::class.java)
    }

    private fun fetchGroup() = viewModel.fetchGroup()

    private fun initClickListeners() {
        closeButton.setOnClickListener { finish() }
        avatar.setOnClickListener { handleAvatarClicked() }
        editGroupPhoto.setOnClickListener { handleAvatarClicked() }
        update.setOnClickListener(saveClickListener)
    }

    private fun handleAvatarClicked() {
        val chooserDialog = ChooserDialog.newInstance()
        chooserDialog.setOnChooserClickListener(object : ChooserDialog.OnChooserClickListener {
            override fun captureImageClicked() = checkCameraPermission()
            override fun importImageFromGalleryClicked() = checkExternalStoragePermission()
        })
        chooserDialog.show(supportFragmentManager, ChooserDialog.TAG)
    }

    private fun checkCameraPermission() {
        PermissionUtil.hasPermission(
                this,
                Manifest.permission.CAMERA,
                PermissionUtil.CAMERA_PERMISSION,
                { startCameraActivity() }
        )
    }

    private fun checkExternalStoragePermission() {
        PermissionUtil.hasPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                PermissionUtil.READ_EXTERNAL_STORAGE_PERMISSION,
                { startGalleryActivity() }
        )
    }

    private val saveClickListener = object : OnSingleClickListener() {
        override fun onSingleClick(v: View?) {
            val groupName = groupName.text.toString()
            val avatarUri = viewModel.avatarUri
            viewModel.updateGroup(avatarUri, groupName)
        }
    }

    private fun initObservers() {
        viewModel.group.observe(this, Observer {
            group -> group?.let { updateUiFromGroup(it) } ?: toast(R.string.unable_to_fetch_group)
        })
        viewModel.isUpdatingGroup.observe(this, Observer {
            isUpdatingGroup -> isUpdatingGroup?.let { loadingSpinner.isVisible(it) }
        })
        viewModel.updatedGroup.observe(this, Observer {
            if (it == true) finish()
        })
        viewModel.error.observe(this, Observer {
            errorMessage -> errorMessage?.let { toast(it) }
        })
    }

    private fun updateUiFromGroup(group: Group) {
        ImageUtil.load(group.avatar, avatar)
        groupName.setText(group.title)
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
        viewModel.avatarUri = when (requestCode) {
            CAPTURE_IMAGE -> Uri.fromFile(File(viewModel.capturedImagePath))
            PICK_IMAGE -> resultIntent?.data
            else -> throw IllegalArgumentException("Unknown requestCode.")
        }
        ImageUtil.renderFileIntoTarget(viewModel.avatarUri, avatar)
    }

    private fun getGroupIdFromIntent() = intent.getStringExtra(EXTRA__GROUP_ID)
}