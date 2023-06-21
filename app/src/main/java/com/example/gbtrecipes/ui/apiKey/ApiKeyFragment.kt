package com.example.gbtrecipes.ui.apiKey

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.gbtrecipes.databinding.FragmentGalleryBinding


class ApiKeyFragment : Fragment() {

private var _binding: FragmentGalleryBinding? = null
  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentGalleryBinding.inflate(inflater, container, false)
    val root: View = binding.root

    val button: Button = binding.button
    val text: EditText = binding.editText
  val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
  val apiKey= sharedPref?.getString("apiKey","")
      text.setText(apiKey)
  button.setOnClickListener(View.OnClickListener { view ->

      val sharedPref = getActivity()?.getPreferences(Context.MODE_PRIVATE)
      if (sharedPref != null) {
          with (sharedPref.edit()) {
              putString("apiKey", text.text.toString())
              apply()
          }
      }

  })
    return root
  }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}