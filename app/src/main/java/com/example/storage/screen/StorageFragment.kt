package com.example.storage.screen

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.storage.R
import com.example.storage.databinding.FragmentStorageBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class StorageFragment : Fragment() {
    private lateinit var binding : FragmentStorageBinding
    private val db = FirebaseFirestore.getInstance()
    private val productExpiryDays = mapOf(
        "Pork" to 5,
        "Chicken" to 2,
        "Root vegetable" to 7,
        "Beef" to 5,
        "Leafy green" to 5,
        "Bell peppers" to 5,
        "Cucumbers" to 5,
        "Zucchini" to 5,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorageBinding.inflate(inflater, container, false)

        addHeaderRow()
        fetchData()

        binding.btnAdd.setOnClickListener { addItemDialog() }

        return binding.root
    }

    private fun addItemDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val productSpinner: Spinner = dialogView.findViewById(R.id.spinnerProduct)
        val qtyET: EditText = dialogView.findViewById(R.id.quantityET)

        val productList = listOf("Select a product", "Pork", "Chicken", "Root vegetable", "Beef", "Leafy green", "Bell peppers", "Cucumbers", "Zucchini")
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, productList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        productSpinner.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            binding.progressbar.visibility = View.VISIBLE
            val selected = productSpinner.selectedItem?.toString() ?: "Select a product"
            val qty = qtyET.text?.toString() ?: ""

            if (selected == "Select a product" || selected.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a product", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                return@setOnClickListener
            }

            if (qty.isEmpty() || qty.toIntOrNull() == null) {
                Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                return@setOnClickListener
            }

            storeData(selected, qty)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun storeData(product: String, quantity: String) {
        val currentDate = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(Date())
        val expiryDays = productExpiryDays[product] ?: 1
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, expiryDays)
        val expiryDate = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(calendar.time)
        val timestamp = System.currentTimeMillis()

        val data = hashMapOf(
            "purchaseDate" to currentDate,
            "product" to product,
            "quantity" to quantity,
            "expiryDate" to expiryDate,
            "timestamp" to timestamp
        )

        db.collection("storage")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Product added successfully!", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                fetchData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
            }
    }

    private fun addRowToTable(id: String, purchaseDate: String, product: String, quantity: String, expiryDate: String) {
        val tableRow = TableRow(requireContext())
        val rowData = listOf(purchaseDate, product, quantity, expiryDate)

        tableRow.background = ContextCompat.getDrawable(requireContext(), R.drawable.row_border)
        rowData.forEachIndexed { index, cellData ->
            val cell = TextView(requireContext())
            cell.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            if (index == 2) {
                cell.gravity = Gravity.CENTER
            }

            cell.text = cellData
            cell.setPadding(32, 16, 32, 16)
            cell.textSize = 14f
            cell.typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto)
            when {
                isExpired(expiryDate) -> {
                    cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
                isExpiringTomorrow(expiryDate) -> {
                    cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                }
                else -> {
                    cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
            }

            tableRow.addView(cell)
        }

        val edit = TextView(requireContext()).apply {
            text = "Edit"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_500))
            textSize = 14f
            setPadding(32, 16, 32, 16)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)
            setOnClickListener { handleEdit(id, product, quantity, expiryDate) }
        }
        tableRow.addView(edit)

        val delete = TextView(requireContext()).apply {
            text = "Delete"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            textSize = 14f
            setPadding(32, 16, 32, 16)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)
            setOnClickListener { handleDelete(id) }
        }
        tableRow.addView(delete)

        binding.tableLayout.addView(tableRow)
    }

    private fun isExpired(expiryDate: String): Boolean {
        val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
        val expiry = dateFormat.parse(expiryDate)
        return expiry?.before(Date()) == true
    }

    private fun isExpiringTomorrow(expiryDate: String): Boolean {
        val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
        val expiry = dateFormat.parse(expiryDate) ?: return false

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = dateFormat.format(calendar.time)

        return dateFormat.format(expiry) == tomorrow
    }

    private fun handleDelete(id: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
            binding.progressbar.visibility = View.VISIBLE
            db.collection("storage").document(id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Product deleted successfully!", Toast.LENGTH_SHORT).show()
                    binding.progressbar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressbar.visibility = View.GONE
                }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun handleEdit(id: String, oldProduct: String, oldQuantity: String, oldExpiryDate: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_item, null)
        val productSpinner: Spinner = dialogView.findViewById(R.id.spinnerProduct)
        val qtyET: EditText = dialogView.findViewById(R.id.quantityET)
        val expiryDateET: EditText = dialogView.findViewById(R.id.expiryDateET)

        val productList = listOf("Select a product", "Pork", "Chicken", "Root vegetable", "Beef", "Leafy green", "Bell peppers", "Cucumbers", "Zucchini")

        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, productList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productSpinner.adapter = adapter

        val productIndex = productList.indexOf(oldProduct)
        productSpinner.setSelection(if (productIndex != -1) productIndex else 0)
        qtyET.setText(oldQuantity)
        expiryDateET.setText(oldExpiryDate)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            binding.progressbar.visibility = View.VISIBLE
            val selectedProduct = productSpinner.selectedItem?.toString() ?: "Select a product"
            val qty = qtyET.text?.toString() ?: ""
            val expiryDate = expiryDateET.text?.toString() ?: ""

            if (selectedProduct == "Select a product" || selectedProduct.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a product", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                return@setOnClickListener
            }

            if (qty.isEmpty() || qty.toIntOrNull() == null) {
                Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                return@setOnClickListener
            }

            updateData(id, selectedProduct, qty, expiryDate)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateData(id: String, product: String, quantity: String, expiryDate: String) {
        val currentDate = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(Date())

        if (expiryDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an expiry date", Toast.LENGTH_SHORT).show()
            binding.progressbar.visibility = View.GONE
            return
        }

        val data: Map<String, Any> = hashMapOf(
            "purchaseDate" to currentDate,
            "product" to product,
            "quantity" to quantity,
            "expiryDate" to expiryDate
        )

        db.collection("storage").document(id)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Product updated successfully!", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                fetchData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
            }
    }

    private fun addHeaderRow() {
        val headerRow = TableRow(requireContext())
        val headers = listOf("Purchase Date", "Product", "Quantity", "Expiry Date", "", "")

        headers.forEach { header ->
            val headerCell = TextView(requireContext())
            headerCell.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            headerCell.setPadding(32, 16, 32, 16)
            headerCell.text = header
            headerCell.textSize = 16f
            headerCell.typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            headerCell.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_500))
            headerCell.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            headerRow.addView(headerCell)
        }

        binding.tableLayout.addView(headerRow)
    }

    private fun fetchData() {
        db.collection("storage")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    if (binding.tableLayout.childCount > 1) {
                        binding.tableLayout.removeViews(1, binding.tableLayout.childCount - 1)
                    }
                    displayData(snapshot)
                } else {
                    Log.d("StorageFragment", "Current data: null")
                }
            }
    }

    private fun displayData(documents: QuerySnapshot) {
        for (document in documents) {
            val id = document.id
            val purchaseDate = document.getString("purchaseDate") ?: ""
            val product = document.getString("product") ?: ""
            val quantity = document.getString("quantity") ?: ""
            val expiryDate = document.getString("expiryDate") ?: ""

            addRowToTable(id, purchaseDate, product, quantity, expiryDate)
        }
    }

}