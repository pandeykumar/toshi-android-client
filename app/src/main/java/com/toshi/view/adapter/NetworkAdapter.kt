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

package com.toshi.view.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.toshi.R
import com.toshi.model.local.network.Network
import com.toshi.view.adapter.viewholder.NetworkViewHolder

class NetworkAdapter(
        private val onItemClickedListener: (Network) -> Unit
) : RecyclerView.Adapter<NetworkViewHolder>() {

    private val networks by lazy { mutableListOf<Network>() }

    private var currentSelectedItem = 0
    private var previousSelectedItem = 0

    fun setItems(networks: List<Network>) {
        this.networks.clear()
        this.networks.addAll(networks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item__network, parent, false)
        return NetworkViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        val network = networks[position]
        val isSelected = currentSelectedItem == position
        holder.apply {
            setNetwork(network)
            if (isSelected) setSelected() else setUnselected()
            setOnItemClickedListener(network, position) { wallet, pos ->
                handleSelectedItem(wallet, pos)
            }
        }
    }

    private fun handleSelectedItem(network: Network, position: Int) {
        previousSelectedItem = currentSelectedItem
        currentSelectedItem = position
        onItemClickedListener(network)
        notifyItemChanged(previousSelectedItem)
    }

    override fun getItemCount(): Int = networks.size
}