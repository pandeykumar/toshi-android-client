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

package com.toshi.view.adapter.viewholder

import android.support.v7.widget.RecyclerView
import android.view.View
import com.toshi.model.local.network.Network
import kotlinx.android.synthetic.main.list_item__network.view.checkbox
import kotlinx.android.synthetic.main.list_item__network.view.name

class NetworkViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
    fun setNetwork(network: Network) {
        itemView.name.text = network.name
    }

    fun setSelected() {
        itemView.checkbox.isChecked = true
    }

    fun setUnselected() {
        itemView.checkbox.isChecked = false
    }

    fun setOnItemClickedListener(wallet: Network, position: Int, listener: (Network, Int) -> Unit) {
        itemView.setOnClickListener {
            updateSelectedState()
            listener(wallet, position)
        }
    }

    private fun updateSelectedState() {
        itemView.checkbox.isChecked = !itemView.checkbox.isChecked
    }
}