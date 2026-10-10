package com.lanlan3292.resolutionchanger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanlan3292.resolutionchanger.databinding.FragmentResolutionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ResolutionFragment : Fragment() {

    private var _binding: FragmentResolutionBinding? = null
    private val binding get() = _binding!!

    private val options = listOf(
        ResolutionOption("2k", R.string.option_2k, 1440, 3200, 560),
        ResolutionOption("1080p", R.string.option_1080p, 1080, 2400, 420),
        ResolutionOption("720p", R.string.option_720p, 720, 1600, 280),
        ResolutionOption("reset", R.string.option_reset, 0, 0, 0, isReset = true)
    )

    private var selectedId: String? = null
    private lateinit var adapter: OptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResolutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OptionAdapter(options) { option ->
            onOptionSelected(option)
        }
        binding.recyclerOptions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOptions.adapter = adapter

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        checkShizuku()
    }

    private fun checkShizuku() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(requireContext(), R.string.toast_shizuku_not_running, Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() != 0) {
            Shizuku.requestPermission(1001)
        } else {
            ShizukuHelper.bindUserService()
        }
    }

    private fun onOptionSelected(option: ResolutionOption) {
        if (!ShizukuHelper.isReady()) {
            Toast.makeText(requireContext(), R.string.toast_shizuku_not_ready, Toast.LENGTH_SHORT).show()
            ShizukuHelper.requestPermission()
            return
        }

        selectedId = option.id
        adapter.notifyDataSetChanged()

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                if (option.isReset) {
                    ShizukuHelper.resetResolution()
                } else {
                    ShizukuHelper.setResolution(option.width, option.height, option.density)
                }
            }
            withContext(Dispatchers.Main) {
                val msg = if (success) {
                    if (option.isReset) getString(R.string.toast_reset_success)
                    else getString(R.string.toast_switch_success, getString(option.titleRes))
                } else {
                    getString(R.string.toast_failed)
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class OptionAdapter(
        private val items: List<ResolutionOption>,
        private val onClick: (ResolutionOption) -> Unit
    ) : RecyclerView.Adapter<OptionAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val radio: RadioButton = view.findViewById(R.id.radio)
            val title: TextView = view.findViewById(R.id.tv_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_resolution_option, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.setText(item.titleRes)
            holder.radio.isChecked = item.id == selectedId
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
