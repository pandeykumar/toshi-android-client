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

package com.toshi.view.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.toshi.R
import com.toshi.extensions.getColor
import com.toshi.extensions.isVisible
import com.toshi.extensions.toast
import com.toshi.manager.model.ERC20TokenPaymentTask
import com.toshi.manager.model.ExternalPaymentTask
import com.toshi.manager.model.PaymentTask
import com.toshi.manager.model.ToshiPaymentTask
import com.toshi.manager.model.W3PaymentTask
import com.toshi.model.local.CurrencyMode
import com.toshi.model.local.User
import com.toshi.model.network.Balance
import com.toshi.util.EthUtil
import com.toshi.util.ImageUtil
import com.toshi.util.PaymentType
import com.toshi.util.logging.LogUtil
import com.toshi.view.fragment.dialogFragment.PaymentConfirmationType
import com.toshi.viewModel.PaymentConfirmationViewModel
import kotlinx.android.synthetic.main.fragment_payment_confirmation.ERC20Amount
import kotlinx.android.synthetic.main.fragment_payment_confirmation.ERC20GasPrice
import kotlinx.android.synthetic.main.fragment_payment_confirmation.ERC20GasPriceFiat
import kotlinx.android.synthetic.main.fragment_payment_confirmation.ERC20PaymentInfoWrapper
import kotlinx.android.synthetic.main.fragment_payment_confirmation.amount
import kotlinx.android.synthetic.main.fragment_payment_confirmation.avatar
import kotlinx.android.synthetic.main.fragment_payment_confirmation.closeButton
import kotlinx.android.synthetic.main.fragment_payment_confirmation.convertedAmount
import kotlinx.android.synthetic.main.fragment_payment_confirmation.convertedGasPrice
import kotlinx.android.synthetic.main.fragment_payment_confirmation.convertedTotal
import kotlinx.android.synthetic.main.fragment_payment_confirmation.dappFavicon
import kotlinx.android.synthetic.main.fragment_payment_confirmation.dappHeader
import kotlinx.android.synthetic.main.fragment_payment_confirmation.dappUrl
import kotlinx.android.synthetic.main.fragment_payment_confirmation.dappWrapper
import kotlinx.android.synthetic.main.fragment_payment_confirmation.displayName
import kotlinx.android.synthetic.main.fragment_payment_confirmation.ethPaymentInfoWrapper
import kotlinx.android.synthetic.main.fragment_payment_confirmation.externalAddress
import kotlinx.android.synthetic.main.fragment_payment_confirmation.externalWrapper
import kotlinx.android.synthetic.main.fragment_payment_confirmation.gasPrice
import kotlinx.android.synthetic.main.fragment_payment_confirmation.loadingOverlay
import kotlinx.android.synthetic.main.fragment_payment_confirmation.loadingSpinner
import kotlinx.android.synthetic.main.fragment_payment_confirmation.pay
import kotlinx.android.synthetic.main.fragment_payment_confirmation.recipientWrapper
import kotlinx.android.synthetic.main.fragment_payment_confirmation.statusMessage
import kotlinx.android.synthetic.main.fragment_payment_confirmation.successfulState
import kotlinx.android.synthetic.main.fragment_payment_confirmation.toolbarTitle
import kotlinx.android.synthetic.main.fragment_payment_confirmation.total

class PaymentConfirmationFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: PaymentConfirmationViewModel
    private var onPaymentApprovedListener: ((PaymentTask) -> Unit)? = null
    private var onPaymentCanceledListener: ((String?) -> Unit)? = null
    private var onFinishedListener: (() -> Unit)? = null
    private var approvedPayment = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_payment_confirmation, container, false)
    }

    override fun onStart() {
        super.onStart()
        setHeight()
    }

    private fun setHeight() {
        dialog?.let {
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = if (isW3Payment()) ViewGroup.LayoutParams.WRAP_CONTENT
            else ViewGroup.LayoutParams.MATCH_PARENT
        }

        view?.post {
            val parent = view?.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val bottomSheetBehavior = params.behavior as BottomSheetBehavior?
            bottomSheetBehavior?.peekHeight = view?.measuredHeight ?: 0
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = init()

    private fun init() {
        initViewModel()
        initClickListeners()
        setTitle()
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(PaymentConfirmationViewModel::class.java)
        viewModel.init(arguments)
    }

    private fun initClickListeners() {
        closeButton.setOnClickListener { handlePaymentCanceled() }
        pay.setOnClickListener { handlePaymentApproved() }
    }

    private fun handlePaymentCanceled() {
        approvedPayment = false
        dismiss()
    }

    private fun handlePaymentApproved() {
        val paymentTask = viewModel.paymentTask.value ?: return
        if (onPaymentApprovedListener != null) approveAndDismiss(paymentTask)
        else viewModel.sendPayment(paymentTask)
    }

    private fun approveAndDismiss(paymentTask: PaymentTask) {
        approvedPayment = true
        onPaymentApprovedListener?.invoke(paymentTask)
        dismiss()
    }

    private fun setTitle() {
        val paymentType = viewModel.getPaymentType()
        val title = if (paymentType == PaymentType.TYPE_SEND) getString(R.string.payment_confirmation_title)
        else getString(R.string.payment_request_confirmation_title)
        toolbarTitle.text = title
    }

    private fun initObservers() {
        viewModel.isLoading.observe(this, Observer {
            isLoading -> isLoading?.let { handleLoadingVisibility(it) }
        })
        viewModel.balance.observe(this, Observer {
            formattedBalance -> formattedBalance?.let { handleBalance(it) }
        })
        viewModel.paymentTask.observe(this, Observer {
            paymentTask -> paymentTask?.let { handlePaymentTask(it) }
        })
        viewModel.paymentTaskError.observe(this, Observer {
            paymentTaskError -> paymentTaskError?.let { handlePaymentTaskError() }
        })
        viewModel.paymentSuccess.observe(this, Observer {
            if (it != null) handleSuccessfulPayment()
        })
        viewModel.paymentError.observe(this, Observer {
            if (it != null) handlePaymentError(it)
        })
        viewModel.finish.observe(this, Observer {
            if (it != null) finishAndDismiss()
        })
    }

    private fun handleLoadingVisibility(isLoading: Boolean) {
        loadingOverlay.isVisible(isLoading)
        loadingSpinner.isVisible(isLoading)
    }

    private fun handleBalance(currentBalance: Balance) {
        renderBalance(currentBalance)
        compareTotalAmountAndBalance()
    }

    private fun renderBalance(balance: Balance) {
        val formattedBalance = balance.localBalance
        val statusMessage = getString(R.string.your_balance, formattedBalance)
        setStatusMessage(statusMessage)
    }

    private fun handlePaymentTask(paymentTask: PaymentTask) {
        renderRecipientInfo(paymentTask)
        renderPaymentInfo(paymentTask)
        compareTotalAmountAndBalance()
        if (isW3Payment()) setHeight() //Update the top position of dialog
    }

    private fun renderRecipientInfo(paymentTask: PaymentTask) {
        when (paymentTask) {
            is W3PaymentTask -> renderDappInfo()
            is ToshiPaymentTask -> renderToshiUserInfo(paymentTask.user)
            is ExternalPaymentTask -> renderExternalInfo(paymentTask)
            is ERC20TokenPaymentTask -> renderERC20TokenInfo(paymentTask)
            else -> LogUtil.exception("Unhandled payment $paymentTask")
        }
    }

    private fun renderDappInfo() {
        dappWrapper.isVisible(true)
        dappHeader.text = viewModel.getDappTitle()
        dappUrl.text = viewModel.getDappUrl()
        dappFavicon.setImageBitmap(viewModel.getDappFavicon())
    }

    private fun renderToshiUserInfo(user: User) {
        recipientWrapper.isVisible(true)
        ImageUtil.load(user.avatar, avatar)
        displayName.text = user.displayName
    }

    private fun renderExternalInfo(paymentTask: PaymentTask) {
        externalWrapper.isVisible(true)
        externalAddress.text = paymentTask.payment.toAddress
    }

    private fun renderERC20TokenInfo(paymentTask: PaymentTask) {
        externalWrapper.isVisible(true)
        externalAddress.text = paymentTask.payment.toAddress
    }

    private fun renderPaymentInfo(paymentTask: PaymentTask) {
        when (paymentTask) {
            is ERC20TokenPaymentTask -> renderERC20TokenPaymentInfo(paymentTask)
            else -> renderETHPaymentInfo(paymentTask)
        }
    }

    private fun renderERC20TokenPaymentInfo(paymentTask: ERC20TokenPaymentTask) {
        ERC20PaymentInfoWrapper.isVisible(true)
        ERC20Amount.text = "${paymentTask.tokenValue} ${paymentTask.tokenSymbol}"
        val gasEthAmount = EthUtil.ethAmountToUserVisibleString(paymentTask.gasPrice.ethAmount)
        ERC20GasPrice.text = getString(R.string.eth_amount, gasEthAmount)
        ERC20GasPriceFiat.text = paymentTask.gasPrice.localAmount
    }

    private fun renderETHPaymentInfo(paymentTask: PaymentTask) {
        ethPaymentInfoWrapper.isVisible(true)
        val currencyMode = viewModel.getCurrencyMode()
        when (currencyMode) {
            CurrencyMode.ETH -> renderPaymentInfoInETHMode(paymentTask)
            CurrencyMode.FIAT -> renderPaymentInfoInFiatMode(paymentTask)
        }
    }

    private fun renderPaymentInfoInETHMode(paymentTask: PaymentTask) {
        val currencyMode = CurrencyMode.ETH
        amount.text = viewModel.getPaymentAmount(paymentTask, currencyMode)
        gasPrice.text = viewModel.getGasPrice(paymentTask, currencyMode)
        total.text = viewModel.getTotalAmount(paymentTask, currencyMode)

        val localAmount = viewModel.getPaymentAmount(paymentTask, CurrencyMode.FIAT)
        convertedAmount.text = localAmount
        val localGasAmount = viewModel.getGasPrice(paymentTask, CurrencyMode.FIAT)
        convertedGasPrice.text = localGasAmount
        val totalAmount = viewModel.getTotalAmount(paymentTask, CurrencyMode.FIAT)
        convertedTotal.text = totalAmount
    }

    private fun renderPaymentInfoInFiatMode(paymentTask: PaymentTask) {
        val currencyMode = CurrencyMode.FIAT
        amount.text = viewModel.getPaymentAmount(paymentTask, currencyMode)
        gasPrice.text = viewModel.getGasPrice(paymentTask, currencyMode)
        total.text = viewModel.getTotalAmount(paymentTask, currencyMode)

        val localAmount = viewModel.getPaymentAmount(paymentTask, CurrencyMode.ETH)
        convertedAmount.text = localAmount
        val localGasAmount = viewModel.getGasPrice(paymentTask, CurrencyMode.ETH)
        convertedGasPrice.text = localGasAmount
        val totalAmount = viewModel.getTotalAmount(paymentTask, CurrencyMode.ETH)
        convertedTotal.text = totalAmount
    }

    private fun handlePaymentTaskError() {
        ethPaymentInfoWrapper.isVisible(false)
        disablePayButton()
        setStatusMessage(getString(R.string.gas_price_error), R.color.error_color)
    }

    private fun handleSuccessfulPayment() {
        loadingOverlay.isVisible(true)
        successfulState.isVisible(true)
        viewModel.finishActivityWithDelay()
    }

    private fun handlePaymentError(errorMessage: Int) {
        loadingOverlay.isVisible(false)
        successfulState.isVisible(false)
        toast(errorMessage)
    }

    private fun finishAndDismiss() {
        approvedPayment = true
        onFinishedListener?.invoke()
        dismiss()
    }

    private fun compareTotalAmountAndBalance() {
        val currentBalance = viewModel.balance.value
        val totalAmount = viewModel.paymentTask.value
        if (currentBalance != null && totalAmount != null) {
            val currentBalanceEth = EthUtil.weiToEth(currentBalance.unconfirmedBalance)
            val totalAmountEth = totalAmount.totalAmount.ethAmount
            if (currentBalanceEth >= totalAmountEth) {
                enablePayButton()
                return
            } else {
                renderInsufficientBalance()
            }
        }

        disablePayButton()
    }

    private fun disablePayButton() {
        pay.isClickable = false
        pay.setBackgroundResource(R.drawable.background_with_radius_disabled)
    }

    private fun enablePayButton() {
        pay.isClickable = true
        pay.setBackgroundResource(R.drawable.background_with_radius_primary_color)
    }

    private fun renderInsufficientBalance() {
        val insufficientBalance = viewModel.balance.value?.localBalance
        insufficientBalance?.let {
            val statusMessage = getString(R.string.insufficient_balance, it)
            setStatusMessage(statusMessage, R.color.error_color)
        }
    }

    private fun setStatusMessage(text: String, color: Int = R.color.textColorPrimary) {
        statusMessage.text = text
        statusMessage.setTextColor(getColor(color) ?: 0)
    }

    // Set the approve listener to null if you want PaymentConfirmation to send the transaction
    // or you can handle it by yourself.
    fun setOnPaymentConfirmationApprovedListener(listener: ((PaymentTask) -> Unit)?): PaymentConfirmationFragment {
        this.onPaymentApprovedListener = listener
        return this
    }

    // Called when the transaction is finished and successful
    fun setOnPaymentConfirmationFinishedListener(listener: (() -> Unit)?): PaymentConfirmationFragment {
        this.onFinishedListener = listener
        return this
    }

    fun setOnPaymentConfirmationCanceledListener(listener: (String?) -> Unit): PaymentConfirmationFragment {
        this.onPaymentCanceledListener = listener
        return this
    }

    override fun onPause() {
        super.onPause()
        dismissAllowingStateLoss()
    }

    companion object {
        const val TAG = "PaymentConfirmationFragment"
        const val CALLBACK_ID = "callback_id"
        const val CONFIRMATION_TYPE = "confirmation_type"
        const val ETH_AMOUNT = "eth_amount"
        const val MEMO = "memo"
        const val PAYMENT_ADDRESS = "payment_address"
        const val PAYMENT_TYPE = "payment_type"
        const val TOSHI_ID = "toshi_id"
        const val UNSIGNED_TRANSACTION = "unsigned_transaction"
        const val DAPP_TITLE = "dapp_title"
        const val DAPP_URL = "dapp_url"
        const val DAPP_FAVICON = "dapp_favicon"
        const val TOKEN_ADDRESS = "token_address"
        const val TOKEN_SYMBOL = "token_symbol"
        const val TOKEN_DECIMALS = "token_decimals"
        const val SEND_MAX_AMOUNT = "send_max_amount"
        const val CURRENCY_MODE = "currency"

        fun newInstanceToshiPayment(toshiId: String,
                                    value: String,
                                    memo: String?,
                                    @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.TOSHI)
                putString(TOSHI_ID, toshiId)
            }
            return newInstance(bundle, value, memo, paymentType)
        }

        fun newInstanceExternalPayment(paymentAddress: String,
                                       value: String,
                                       memo: String? = null,
                                       @PaymentType.Type paymentType: Int,
                                       sendMaxAmount: Boolean = false,
                                       currencyMode: CurrencyMode = CurrencyMode.FIAT): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.EXTERNAL)
                putString(PAYMENT_ADDRESS, paymentAddress)
                putBoolean(SEND_MAX_AMOUNT, sendMaxAmount)
                putSerializable(CURRENCY_MODE, currencyMode)
            }
            return newInstance(bundle, value, memo, paymentType)
        }

        fun newInstanceERC20TokenPayment(toAddress: String,
                                         value: String,
                                         tokenAddress: String,
                                         tokenSymbol: String,
                                         tokenDecimals: Int,
                                         memo: String?,
                                         @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.EXTERNAL)
                putString(PAYMENT_ADDRESS, toAddress)
                putString(TOKEN_ADDRESS, tokenAddress)
                putString(TOKEN_SYMBOL, tokenSymbol)
                putInt(TOKEN_DECIMALS, tokenDecimals)
            }
            return newInstance(bundle, value, memo, paymentType)
        }

        fun newInstanceWebPayment(unsignedTransaction: String,
                                  paymentAddress: String,
                                  value: String?,
                                  callbackId: String,
                                  memo: String?,
                                  url: String?,
                                  title: String?,
                                  favIcon: Bitmap?): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.WEB)
                putString(UNSIGNED_TRANSACTION, unsignedTransaction)
                putString(PAYMENT_ADDRESS, paymentAddress)
                putString(CALLBACK_ID, callbackId)
                putString(DAPP_URL, url)
                putString(DAPP_TITLE, title)
                putParcelable(DAPP_FAVICON, favIcon)
            }
            return newInstance(bundle, value, memo, PaymentType.TYPE_SEND)
        }

        private fun newInstance(bundle: Bundle,
                                value: String?,
                                memo: String?,
                                @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            bundle.apply {
                putString(ETH_AMOUNT, value)
                putString(MEMO, memo)
                putInt(PAYMENT_TYPE, paymentType)
            }
            val fragment = PaymentConfirmationFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun isW3Payment(): Boolean {
        val confirmationType = arguments?.getInt(CONFIRMATION_TYPE)
        return confirmationType == PaymentConfirmationType.WEB
    }

    override fun onDismiss(dialog: DialogInterface?) {
        val callbackId = viewModel.getCallbackId()
        if (!approvedPayment) onPaymentCanceledListener?.invoke(callbackId)
        super.onDismiss(dialog)
    }
}