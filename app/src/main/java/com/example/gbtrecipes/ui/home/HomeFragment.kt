package com.example.gbtrecipes.ui.home

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.example.gbtrecipes.databinding.FragmentHomeBinding
import com.example.gbtrecipes.ui.recipe.RecipeFragment


class HomeFragment : Fragment() {

private var _binding: FragmentHomeBinding? = null
  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    val root: View = binding.root
    val button: Button = binding.authenticate

    val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
    val name= sharedPref?.getString("apiKey","")
//    val textView: TextView = binding.textHome
    homeViewModel.text.observe(viewLifecycleOwner) {
      if(name.equals("")){
      }
      else{
//          textView.text = "What do you want to make"
      }
    }
      button.isEnabled = false
      binding.recipe.addTextChangedListener(object : TextWatcher {
          override fun afterTextChanged(s: Editable?) {
              // Check if the EditText is empty
              val isEditTextEmpty = s?.toString()?.trim().isNullOrEmpty()

              // Enable or disable the button based on the EditText state
              button.isEnabled = !isEditTextEmpty
          }

          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
              // Not used
          }

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
              // Not used
          }
      })
      button.setOnClickListener {
          var recipeName = binding.recipe.text.toString()
          val someFragment: Fragment = RecipeFragment()
          val args = Bundle()
          args.putString("dishName", recipeName)
          someFragment.setArguments(args)
          val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
          transaction.replace(
              com.example.gbtrecipes.R.id.nav_host_fragment_content_main,
              someFragment
          )

          transaction.addToBackStack("home") // when not blank, this transaction will be added to backstack

          transaction.commit()
      }
    return root
  }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}