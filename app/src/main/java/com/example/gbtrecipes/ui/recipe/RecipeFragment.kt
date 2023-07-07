package com.example.gbtrecipes.ui.recipe
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gbtrecipes.R
import com.example.gbtrecipes.databinding.FragmentRecipeBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class Ingredient(
    val name: String,
    val unit: String,
    val amount: Float
)

data class Recipe(
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val time_to_cook: Int
)
class IngredientAdapter(private val ingredientList: List<Ingredient>) : RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val unitTextView: TextView = itemView.findViewById(R.id.unitTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredientList[position]
        holder.nameTextView.text = ingredient.name
        holder.unitTextView.text = ingredient.unit
        holder.amountTextView.text = ingredient.amount.toString()
    }

    override fun getItemCount(): Int {
        return ingredientList.size
    }
}

class InstructionAdapter(private val instructionList: List<String>) : RecyclerView.Adapter<InstructionAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instructionTextView: TextView = itemView.findViewById(R.id.instructionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_instruction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val instruction = instructionList[position]
        holder.instructionTextView.text = instruction
    }

    override fun getItemCount(): Int {
        return instructionList.size
    }
}

class RecipeFragment : Fragment() {

private var _binding: FragmentRecipeBinding? = null
  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
      val dishName = requireArguments().getString("dishName", "")
      val dietaryRestrictions = requireArguments().getString("dietaryRestrictions", "")
      val metric = requireArguments().getString("metric", "false").toBoolean()
      val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
      val apiKey= sharedPref?.getString("apiKey","")


    _binding = FragmentRecipeBinding.inflate(inflater, container, false)
    val root: View = binding.root

      GlobalScope.launch(Dispatchers.Main) {
          // Show the loading bar while we query the openAI api
          val progressDialog = ProgressDialog(context)
          progressDialog.setMessage("Loading...")
          progressDialog.setCancelable(false)
          progressDialog.show()
          _binding!!.dishNameTextView.text = dishName.capitalize()
          try {
              val response = withContext(Dispatchers.IO) {
                  dishName?.let { makeOpenAiRequest(it, dietaryRestrictions!!, apiKey!!, metric!!) }
              }

              val recipe = response?.let { parseRecipeFromJson(it) }

              val recyclerView: RecyclerView = binding.ingredientsRecyclerView
              val adapter = recipe?.let { IngredientAdapter(it.ingredients) }
              recyclerView.adapter = adapter
              recyclerView.layoutManager = LinearLayoutManager(context)

              val recyclerViewForInstructions: RecyclerView = binding.instructionsRecyclerView
              val adapterForInstructions = recipe?.let { InstructionAdapter(it.instructions) }
              recyclerViewForInstructions.adapter = adapterForInstructions
              recyclerViewForInstructions.layoutManager = LinearLayoutManager(context)
              if (recipe != null) {
                  var cookTime = recipe.time_to_cook.toString()
                  _binding!!.cookTimeTextView.text = "Cook Time: $cookTime Minutes"
              }
          }catch (e: java.lang.Exception){
              val errorMessage = "An error occurred: ${e.message}"
              // Display a dialog
              AlertDialog.Builder(context)
                  .setTitle("Error")
                  .setMessage(errorMessage)
                  .setPositiveButton("OK", null)
                  .show()
          }finally {
              // Dismiss the loading bar
              progressDialog.dismiss()
          }



      }


      return root
  }
    fun parseRecipeFromJson(jsonString: String): Recipe {
        val type = object : TypeToken<Recipe>() {}.type
        return Gson().fromJson<Recipe?>(jsonString, type)
    }
override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun makeOpenAiRequest(recipeName: String, dietaryRestrictions: String, token: String, metric: Boolean): String{
        val url = URL("https://api.openai.com/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        val username = "your_username"
        val password = token
        val credentials = "$username:$password"
        val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        connection.setRequestProperty("Authorization", "Basic $encodedCredentials")
        connection.doOutput = true
        var instructions = "Provide a recipe for $recipeName"
        if(!dietaryRestrictions.equals("")){
            instructions = "Provide a recipe for $recipeName, dietary restrictions are no $dietaryRestrictions"
        }
        var unitType = "imperial"
        if (metric) {
            unitType = "metric"
        }
        var finalInstructions = "$instructions, use the $unitType system for measurements"
        val requestBody = """
        {
            "model": "gpt-3.5-turbo-0613",
            "messages": [
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "$finalInstructions"}
            ],
            "functions": [
                {
                    "name": "set_recipe",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "ingredients": {
                                "type": "array",
                                "items": {
                                    "type": "object",
                                    "properties": {
                                        "name": { "type": "string" },
                                        "unit": {
                                            "type": "string",
                                            "enum": ["grams", "ml", "cups", "pieces", "teaspoons"]
                                        },
                                        "amount": { "type": "number" }
                                    },
                                    "required": ["name", "unit", "amount"]
                                }
                            },
                            "instructions": {
                                "type": "array",
                                "description": "Steps to prepare the recipe (no numbering)",
                                "items": { "type": "string" }
                            },
                            "time_to_cook": {
                                "type": "number",
                                "description": "Total time to prepare the recipe in minutes"
                            }
                        },
                        "required": ["ingredients", "instructions", "time_to_cook"]
                    }
                }
            ],
            "temperature": 0
        }
    """.trimIndent()

        val outputStream = DataOutputStream(connection.outputStream)
        outputStream.writeBytes(requestBody)
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        val mes = connection.responseMessage

        if (responseCode != HttpURLConnection.HTTP_OK) {
            // Handle other non-OK response codes here
            throw Exception("HTTP error code: $responseCode")
        }
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val responseBody = response.toString()
        val jsonResponse = JSONObject(responseBody)
        val choicesArray = jsonResponse.getJSONArray("choices")
        val firstChoice = choicesArray.getJSONObject(0)
        val messageObject = firstChoice.getJSONObject("message")
        val functionCallObject = messageObject.optJSONObject("function_call")
        val arguments = functionCallObject?.getString("arguments")

        connection.disconnect()
        return arguments.toString()

    }
}
