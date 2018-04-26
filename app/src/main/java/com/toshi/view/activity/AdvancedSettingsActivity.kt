/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 *  This program is free software: you can redistribute it and/or modify
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

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.toshi.BuildConfig
import com.toshi.R
import com.toshi.extensions.getViewModel
import com.toshi.extensions.isVisible
import com.toshi.extensions.startActivity
import com.toshi.extensions.startActivityForResult
import com.toshi.extensions.toast
import com.toshi.model.local.network.Network
import com.toshi.model.local.network.Networks
import com.toshi.util.BuildTypes
import com.toshi.util.ScannerResultType
import com.toshi.viewModel.AdvancedSettingsViewModel
import kotlinx.android.synthetic.main.activity_settings_advanced.closeButton
import kotlinx.android.synthetic.main.activity_settings_advanced.currentNetwork
import kotlinx.android.synthetic.main.activity_settings_advanced.currentNetworkWrapper
import kotlinx.android.synthetic.main.activity_settings_advanced.loadingSpinner
import kotlinx.android.synthetic.main.activity_settings_advanced.networkSwitcherWrapper
import kotlinx.android.synthetic.main.activity_settings_advanced.openSourceLicenses
import kotlinx.android.synthetic.main.activity_settings_advanced.version

class AdvancedSettingsActivity : AppCompatActivity() {

    companion object {
        private const val SCAN_REQUEST_CODE = 200
    }

    private lateinit var viewModel: AdvancedSettingsViewModel
    private var scannerCounter = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_advanced)
        init()
    }

    private fun init() {
        initViewModel()
        initCLickListeners()
        setVersionName()
        setNetworkSwitcherVisibility()
        setCurrentNetwork(Networks.getInstance().currentNetwork)
        initObservers()
    }

    private fun initViewModel() {
        viewModel = getViewModel()
    }

    private fun initCLickListeners() {
        version.setOnClickListener { handleVersionClicked() }
        closeButton.setOnClickListener { finish() }
        currentNetworkWrapper.setOnClickListener {}
        openSourceLicenses.setOnClickListener { startActivity<LicenseListActivity>() }
    }

    private fun handleVersionClicked() {
        scannerCounter++
        if (scannerCounter % 10 == 0) startScannerActivity()
    }

    private fun startScannerActivity() {
        startActivityForResult<ScannerActivity>(SCAN_REQUEST_CODE) {
            putExtra(ScannerActivity.SCANNER_RESULT_TYPE, ScannerResultType.NO_ACTION)
        }
    }

    private fun setVersionName() {
        val versionName = BuildConfig.VERSION_NAME
        val appVersion = getString(R.string.app_version, versionName)
        version.text = appVersion
    }

    private fun setNetworkSwitcherVisibility() {
        networkSwitcherWrapper.isVisible(BuildConfig.BUILD_TYPE == BuildTypes.DEBUG)
    }

    private fun initObservers() {
        viewModel.network.observe(this, Observer {
            if (it != null) handleNetworkChange(it)
        })
        viewModel.error.observe(this, Observer {
            if (it != null) toast(it)
        })
        viewModel.isLoading.observe(this, Observer {
            if (it != null) loadingSpinner.isVisible(it)
        })
    }

    private fun handleNetworkChange(network: Network) {
        setCurrentNetwork(network)
        toast(R.string.network_changed)
    }

    private fun setCurrentNetwork(network: Network) {
        currentNetwork.text = network.name
    }
}