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

package com.toshi.view.fragment.dialogFragment;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.toshi.R;
import com.toshi.databinding.FragmentRateBinding;

public class RateDialog extends DialogFragment {

    public static final String TAG = "RateDialog";
    private static final String RATING = "rating";
    private static final int MIN_RATE = 1;

    private FragmentRateBinding binding;
    public OnRateDialogClickListener onRateClicked;
    private int rating = MIN_RATE;
    private int titleResource;

    public interface OnRateDialogClickListener {
        void onRateClicked(final int rating, final String review);
    }

    public static RateDialog newInstance(final boolean isApp) {
        final RateDialog dialog = new RateDialog();
        if (isApp) dialog.titleResource = R.string.rate_bot;
        return dialog;
    }

    public RateDialog() {
        titleResource = R.string.rate_this_user;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@NonNull Bundle state) {
        final Dialog dialog = super.onCreateDialog(state);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        dialog.setCanceledOnTouchOutside(true);
        restoreState(state);
        return dialog;
    }

    private void restoreState(final Bundle state) {
        if (state == null) return;
        this.rating = state.getInt(RATING, MIN_RATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rate, container, false);
        initView();
        initClickListeners();
        return this.binding.getRoot();
    }

    private void initView() {
        final String title = getString(titleResource);
        this.binding.title.setText(title);
    }

    private void initClickListeners() {
        this.binding.ratingView.setOnItemClickListener(rating -> this.rating = rating);
        this.binding.rate.setOnClickListener(__ -> handleRateClicked());
        this.binding.noThanks.setOnClickListener(__ -> dismiss());
    }

    private void handleRateClicked() {
        final String review = this.binding.reviewInput.getText().toString();
        this.onRateClicked.onRateClicked(this.rating, review);
        dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(RATING, this.rating);
        super.onSaveInstanceState(outState);
    }
}
