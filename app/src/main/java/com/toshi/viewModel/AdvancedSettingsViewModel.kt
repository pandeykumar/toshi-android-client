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

package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.R
import com.toshi.manager.BalanceManager
import com.toshi.model.local.network.Network
import com.toshi.util.SingleLiveEvent
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class AdvancedSettingsViewModel(
        private val balanceManager: BalanceManager = BaseApplication.get().balanceManager
) : ViewModel() {

    val isLoading by lazy { MutableLiveData<Boolean>() }
    val network by lazy { SingleLiveEvent<Network>() }
    val error by lazy { SingleLiveEvent<Int>() }

    private val subscriptions by lazy { CompositeSubscription() }

    fun changeNetwork(network: Network) {
        if (isLoading.value == true) return

        val sub = balanceManager
                .changeNetwork(network)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isLoading.value = true }
                .doOnTerminate { isLoading.value = false }
                .doOnCompleted { balanceManager.refreshBalance() }
                .doOnError { LogUtil.exception(it) }
                .subscribe(
                        { this.network.value = network },
                        { error.value = R.string.network_change_error }
                )

        subscriptions.add(sub)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}