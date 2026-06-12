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

/**
 * Fragment de l'historique des scans.
 *
 * Utilise RecyclerView avec ListAdapter comme dans le cours (chapitre 7).
 * Observe scanHistory (LiveData<List<ScanHistory>>) depuis le ViewModel.
 *
 * @AndroidEntryPoint pour Hilt (cours chapitre 5)
 */
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

        // Initialisation de la RecyclerView (cours chapitre 7)
        scanAdapter = ScanAdapter()
        binding.historyRecyclerView.apply {
            adapter = scanAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Observation de l'historique depuis Room via le ViewModel
        // Room met à jour la LiveData automatiquement à chaque nouveau scan
        fruitViewModel.scanHistory.observe(viewLifecycleOwner) { scans ->
            scanAdapter.submitList(scans)

            // Affiche le message "historique vide" si nécessaire
            if (scans.isEmpty()) {
                binding.emptyHistoryText.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyHistoryText.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
            }
        }

        // Bouton suppression de l'historique (FloatingActionButton)
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

    // =========================================================================
    // RecyclerView Adapter (cours chapitre 7)
    // =========================================================================

    /**
     * Adapter de la RecyclerView pour l'historique des scans.
     * Utilise ListAdapter + DiffUtil comme recommandé dans le cours (chapitre 7).
     */
    inner class ScanAdapter : ListAdapter<ScanHistory, ScanAdapter.ScanViewHolder>(ScanDiffCallback()) {

        /**
         * Crée le ViewHolder en gonflant le layout view_holder_scan.xml
         * Comme dans le cours chapitre 7.
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
            val binding = ViewHolderScanBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ScanViewHolder(binding)
        }

        /**
         * Remplit le ViewHolder avec les données du scan.
         */
        override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        /**
         * ViewHolder : représente un élément de la liste.
         * Comme dans le cours chapitre 7.
         */
        inner class ScanViewHolder(
            private val binding: ViewHolderScanBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(scan: ScanHistory) {
                binding.historyFruitNameText.text = scan.fruitName
                binding.historyConfidenceText.text = getString(
                    R.string.history_confidence_text, scan.confidence * 100
                )
                // Formatage de la date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                binding.historyDateText.text = dateFormat.format(Date(scan.timestamp))
            }
        }
    }

    /**
     * DiffUtil pour optimiser les mises à jour de la RecyclerView.
     * Comme dans le cours chapitre 7.
     */
    class ScanDiffCallback : DiffUtil.ItemCallback<ScanHistory>() {
        override fun areItemsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
            return oldItem == newItem
        }
    }
}
