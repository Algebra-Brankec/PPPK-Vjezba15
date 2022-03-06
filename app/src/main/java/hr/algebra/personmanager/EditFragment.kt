package hr.algebra.personmanager

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import hr.algebra.personmanager.dao.Person
import hr.algebra.personmanager.databinding.FragmentEditBinding
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

const val PERSON_ID = "personId"
private const val IMAGE_TYPE = "image/*"

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private lateinit var person: Person


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchPerson()
        setupListeners()
    }

    private fun fetchPerson() {
        val personId = arguments?.getLong(PERSON_ID)
        if (personId != null) {
            GlobalScope.launch(Dispatchers.Main) { // dispatch in MAIN thread
                person = withContext(Dispatchers.IO) { // work in IO thread
                    (context?.applicationContext as App).getPersonDao().getPerson(personId!!)
                        ?: Person()
                }
                bindPerson()
            }
        } else {
            person = Person()
            bindPerson()
        }
    }

    private fun bindPerson() {
        if (person.picturePath != null) {
            Picasso.get()
                .load(File(person.picturePath))
                .transform(RoundedCornersTransformation(50, 5)) // radius, margin
                .into(binding.ivImage)
        } else {
            binding.ivImage.setImageResource(R.mipmap.ic_launcher)
        }
        binding.tvDate.text = person.birthDate.format(DateTimeFormatter.ISO_DATE)
        binding.etFirstName.setText(person.firstName ?: "")
        binding.etLastName.setText(person.lastName ?: "")
    }

    private fun setupListeners() {
        binding.tvDate.setOnClickListener {
            handleDate()
        }
        binding.ivImage.setOnLongClickListener {
            handleImage()
            true
        }
        binding.btnCommit.setOnClickListener {
            if (formValid()) {
                commit()
            }
        }
        binding.etFirstName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                person.firstName = text?.toString()?.trim()
            }
        })
        binding.etLastName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                person.lastName = text?.toString()?.trim()
            }
        })
    }

    private fun handleDate() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                person.birthDate = LocalDate.of(year, month, dayOfMonth)
                bindPerson()
            },
            person.birthDate.year,
            person.birthDate.monthValue,
            person.birthDate.dayOfMonth
        ).show()
    }

    private fun handleImage() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = IMAGE_TYPE
            imageResult.launch(this)
        }
    }

    private val imageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                if (person.picturePath != null) {
                    File(person.picturePath).delete()
                }
                val dir = context?.applicationContext?.getExternalFilesDir(null)
                val file = File(dir, File.separator.toString() + UUID.randomUUID().toString() + ".jpg")
                context?.contentResolver?.openInputStream(it.data?.data as Uri).use { inputStream ->
                    FileOutputStream(file).use { fos ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val bos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                        fos.write(bos.toByteArray())
                        person.picturePath = file.absolutePath
                        bindPerson()
                    }
                }
            }

        }


    private fun formValid(): Boolean {
        var ok = true
        arrayOf(binding.etFirstName, binding.etLastName).forEach {
            if (it.text.isNullOrEmpty()) {
                it.error = getString(R.string.please_insert_value)
                ok = false
            }
        }
        return ok && person.picturePath != null;
    }

    private fun commit() {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                if (person._id == null) {
                    (context?.applicationContext as App).getPersonDao().insert(person)
                } else {
                    (context?.applicationContext as App).getPersonDao().update(person)
                }
            }
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}