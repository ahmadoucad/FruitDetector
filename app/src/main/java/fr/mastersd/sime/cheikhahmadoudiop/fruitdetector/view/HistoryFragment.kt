// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/view/HistoryFragment.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import androidx.fragment.app.activityViewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.R
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.FragmentHistoryBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.ViewHolderScanBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.ScanHistory
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel.FruitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private val fruitViewModel: FruitViewModel by activityViewModels()

    // Adapter de la RecyclerView
    private lateinit var scanAdapter: ScanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanAdapter = ScanAdapter()
        binding.historyRecyclerView.apply {
            adapter = scanAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }


        fruitViewModel.scanHistory.observe(viewLifecycleOwner) { scans ->
            scanAdapter.submitList(scans)


            if (scans.isEmpty()) {
                binding.emptyHistoryText.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyHistoryText.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
            }
        }


        binding.clearHistoryButton.setOnClickListener {
            // Confirmation avant suppression
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer l'historique")
                .setMessage("Voulez-vous supprimer tous les scans ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    fruitViewModel.clearHistory()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }


    inner class ScanAdapter : ListAdapter<ScanHistory, ScanAdapter.ScanViewHolder>(ScanDiffCallback()) {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
            val binding = ViewHolderScanBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ScanViewHolder(binding)
        }


        override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
            holder.bind(getItem(position))
        }


        inner class ScanViewHolder(
            private val binding: ViewHolderScanBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(scan: ScanHistory) {
                binding.historyFruitNameText.text = scan.fruitName
                binding.historyConfidenceText.text = getString(
                    R.string.history_confidence_text, scan.confidence * 100
                )

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                binding.historyDateText.text = dateFormat.format(Date(scan.timestamp))
            }
        }
    }


    class ScanDiffCallback : DiffUtil.ItemCallback<ScanHistory>() {
        override fun areItemsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
            return oldItem == newItem
        }
    }
}
