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

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.toshi.R
import com.toshi.crypto.util.isPaymentAddressValid
import com.toshi.extensions.isVisible
import com.toshi.extensions.startActivityForResult
import com.toshi.extensions.toast
import com.toshi.model.network.Balance
import com.toshi.model.network.token.EtherToken
import com.toshi.util.DialogUtil
import com.toshi.util.PaymentType
import com.toshi.util.QrCodeHandler
import com.toshi.util.ScannerResultType
import com.toshi.view.adapter.listeners.TextChangedListener
import com.toshi.view.fragment.PaymentConfirmationFragment
import com.toshi.viewModel.SendEtherViewModel
import kotlinx.android.synthetic.main.activity_send_erc20_token.*

class SendETHActivity : AppCompatActivity() {
    companion object {
        private const val PAYMENT_SCAN_REQUEST_CODE = 200
    }

    private lateinit var viewModel: SendEtherViewModel

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.activity_send_erc20_token)
        init()
    }

    private fun init() {
        initViewModel()
        initClickListeners()
        updateUi()
        initObservers()
        initTextListeners()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(SendEtherViewModel::class.java)
    }

    private fun initClickListeners() {
        max.setOnClickListener { setMaxAmount() }
        currencySwitcher.setOnClickListener { viewModel.switchCurrencyMode({ switchFromFiatToEthValue() }, { switchFromEthToFiatValue() }) }
        closeButton.setOnClickListener { finish() }
        qrCodeBtn.setOnClickListener { startScanQrActivity() }
        paste.setOnClickListener { pasteToAddress() }
        continueBtn.setOnClickListener { validateAddressAndShowPaymentConfirmation() }
    }

    private fun setMaxAmount() {
        if (viewModel.isSendingMaxAmount.value == true) {
            viewModel.isSendingMaxAmount.value = false
            toAmount.setText("")
            toAmount.requestFocus()
            return
        }
        showMaxAmountPaymentConfirmation()
    }

    private fun showMaxAmountPaymentConfirmation() {
        DialogUtil.getBaseDialog(
                this,
                R.string.send_max_amount_title,
                R.string.send_max_amount_message,
                R.string.ok,
                R.string.cancel,
                { _, _ -> handleMaxAmountAccepted() },
                { _, _ -> handleMaxAmountCanceled() }
        ).show()
    }

    private fun handleMaxAmountAccepted() {
        viewModel.isSendingMaxAmount.value = true
        val maxAmount = viewModel.getMaxAmount()
        toAmount.setText(maxAmount)
    }

    private fun handleMaxAmountCanceled() {
        viewModel.isSendingMaxAmount.value = false
    }

    private fun switchFromFiatToEthValue() {
        val inputValue = toAmount.text.toString()
        val ethAmount = when {
            viewModel.isSendingMaxAmount.value == true -> viewModel.getMaxAmount()
            inputValue.isNotEmpty() -> viewModel.fiatToEth(inputValue)
            else -> ""
        }
        toAmount.setText(ethAmount)
        toAmount.setSuffix(getString(R.string.eth_currency_code))
        toAmount.setPrefix("")
        toAmount.setSelection(toAmount.text.length)
    }

    private fun switchFromEthToFiatValue() {
        val inputValue = toAmount.text.toString()
        val fiatAmount = when {
            viewModel.isSendingMaxAmount.value == true -> viewModel.getMaxAmount()
            inputValue.isNotEmpty() -> viewModel.ethToFiat(inputValue)
            else -> ""
        }
        toAmount.setText(fiatAmount)
        toAmount.setSuffix(viewModel.getFiatCurrencyCode())
        toAmount.setPrefix(viewModel.getFiatCurrencySymbol())
        toAmount.setSelection(toAmount.text.length)
    }

    private fun startScanQrActivity() = startActivityForResult<ScannerActivity>(PAYMENT_SCAN_REQUEST_CODE) {
        putExtra(ScannerActivity.SCANNER_RESULT_TYPE, ScannerResultType.PAYMENT_ADDRESS)
    }

    private fun pasteToAddress() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData == null || clipData.itemCount == 0) return
        val clipItem = clipData.getItemAt(0)
        if (clipItem == null || clipItem.text == null) return
        val textFromClipboard = clipItem.text.toString()
        toAddress.setText(textFromClipboard)
    }

    private fun validateAddressAndShowPaymentConfirmation() {
        addressError.isVisible(false)
        val address = toAddress.text.toString()
        val amount = toAmount.text.toString()
        val encodedEthAmount = viewModel.getEncodedEthAmount(amount)
        showPaymentConfirmation(encodedEthAmount, address)
    }

    private fun showPaymentConfirmation(value: String, toAddress: String) {
        val dialog = PaymentConfirmationFragment.newInstanceExternalPayment(
                paymentAddress = toAddress,
                value = value,
                paymentType = PaymentType.TYPE_SEND,
                currencyMode = viewModel.currencyMode,
                sendMaxAmount = viewModel.isSendingMaxAmount.value == true
        )
        dialog.show(supportFragmentManager, PaymentConfirmationFragment.TAG)
        dialog.setOnPaymentConfirmationFinishedListener { finish() }
    }

    private fun updateUi() {
        renderToolbar()
        setAmountSuffix()
        showCurrencySwitcher()
        showConvertedAmount()
    }

    private fun showCurrencySwitcher() = currencySwitcher.isVisible(true)

    private fun showConvertedAmount() {
        toAmountConverted.isVisible(true)
        if (toAmountConverted.text.isEmpty()) updateConvertedAmount("0.0")
    }

    private fun setAmountSuffix() {
        val suffix = viewModel.getCurrencyCode()
        toAmount.setSuffix(suffix)
    }

    private fun renderToolbar() {
        val token = EtherToken.getTokenFromIntent(intent)
        if (token == null) {
            toast(R.string.invalid_token)
            finish()
            return
        }
        toolbarTitle.text = getString(R.string.send_token, token.symbol)
    }

    private fun initObservers() {
        viewModel.ethBalance.observe(this, Observer {
            if (it != null) renderEthBalance(it)
        })
        viewModel.isSendingMaxAmount.observe(this, Observer {
            if (it != null) updateMaxButtonState(it)
        })
    }

    private fun renderEthBalance(currentBalance: Balance) {
        val totalEthAmount = viewModel.getTotalEthAmount(currentBalance)
        val ethAmountString = getString(R.string.eth_amount, totalEthAmount)
        val statusMessage = getString(R.string.your_balance_eth_fiat, ethAmountString, currentBalance.localBalance)
        balance.text = statusMessage
    }

    private fun updateMaxButtonState(isSendingMaxAmount: Boolean) {
        if (isSendingMaxAmount) enableMaxAmountButtonAndDisableAmountInput()
        else disableMaxAmountButtonAndEnableAmountInput()
    }

    private fun enableMaxAmountButtonAndDisableAmountInput() {
        max.setBackgroundResource(R.drawable.background_with_round_corners_enabled)
        max.setTextColor(R.color.textColorContrast)
        toAmount.updateTextColor(R.color.textColorHint)
        toAmount.isFocusable = false
        toAmount.isFocusableInTouchMode = false
    }

    private fun disableMaxAmountButtonAndEnableAmountInput() {
        max.setBackgroundResource(R.drawable.background_with_round_corners_disabled)
        max.setTextColor(R.color.textColorPrimary)
        toAmount.updateTextColor(R.color.textColorPrimary)
        toAmount.isFocusable = true
        toAmount.isFocusableInTouchMode = true
    }

    private fun initTextListeners() {
        toAmount.addTextChangedListener(object : TextChangedListener() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateConvertedAmount(s.toString())
                validateAmount(s.toString())
                enableOrDisableContinueButton()
            }
        })
        toAddress.addTextChangedListener(object : TextChangedListener() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateAddress(s.toString())
                enableOrDisableContinueButton()
            }
        })
    }

    private fun updateConvertedAmount(amountInput: String) {
        val convertedValue = viewModel.getConvertedValue(amountInput)
        toAmountConverted.text = convertedValue
    }

    private fun validateAmount(amountInput: String) {
        amountError.isVisible(false)
        if (amountInput.isEmpty()) return
        val hasEnoughBalance = viewModel.hasEnoughBalance(amountInput)
        val isValidAmount = viewModel.isAmountValid(amountInput)
        if (!isValidAmount) showInvalidAmountError()
        if (!hasEnoughBalance) showAmountError()
    }

    private fun validateAddress(addressInput: String) {
        addressError.isVisible(false)
        if (addressInput.isEmpty()) return
        val isAddressValid = isPaymentAddressValid(addressInput)
        if (!isAddressValid) showAddressError()
    }

    private fun enableOrDisableContinueButton() {
        val amount = toAmount.text.toString()
        val address = toAddress.text.toString()
        val isAmountValid = viewModel.isAmountValid(amount) && viewModel.hasEnoughBalance(amount)
        val isAddressValid = isPaymentAddressValid(address)
        if (isAmountValid && isAddressValid) enableContinueButton()
        else disableContinueButton()
    }

    private fun showAmountError() {
        val ethBalance = getString(R.string.eth_balance, viewModel.getEtherBalance())
        amountError.isVisible(true)
        amountError.text = getString(R.string.insufficient_balance, ethBalance)
    }

    private fun showInvalidAmountError() {
        amountError.isVisible(true)
        amountError.text = getString(R.string.invalid_format)
    }

    private fun showAddressError() {
        addressError.isVisible(true)
        addressError.text = getString(R.string.invalid_payment_address)
    }

    private fun enableContinueButton() {
        continueBtn.isEnabled = true
        continueBtn.setBackgroundResource(R.drawable.background_with_radius_primary_color)
    }

    private fun disableContinueButton() {
        continueBtn.isEnabled = false
        continueBtn.setBackgroundResource(R.drawable.background_with_radius_disabled)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultIntent)
        if (requestCode != PAYMENT_SCAN_REQUEST_CODE || resultCode != Activity.RESULT_OK || resultIntent == null) return
        val paymentAddress = resultIntent.getStringExtra(QrCodeHandler.ACTIVITY_RESULT)
        toAddress.setText(paymentAddress)
    }
}