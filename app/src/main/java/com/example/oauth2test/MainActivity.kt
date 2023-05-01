package com.example.oauth2test

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.oauth2test.databinding.ActivityMainBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var accessToken: String = ""
    private var oauthVerifier: String = ""
    //NOTE: Function ONLY RETURNS A STRING of my personal MyAnimeAPI Client ID. You can get your own, by going to:
    //MyAnimeList.net -> login -> Click Username -> Account Settings -> API (top bar) -> create
    private val oauthClientId: String = MyAnimeListAPIClientId().getClientId()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val animeList = mutableListOf<Anime>()
        animeList.add(Anime(24765, "Gakkougurashi!", "https://cdn.myanimelist.net/images/anime/1798/91548l.jpg"))

        binding.rvMain.layoutManager = GridLayoutManager(this, 2)
        val animeAdapter = AnimeAdapter(animeList)
        binding.rvMain.adapter = animeAdapter

        val queue = Volley.newRequestQueue(this)
        binding.btnLogin.setOnClickListener {
            try {
                //Creates a 50-letter random string
                val sr = SecureRandom()
                val code = ByteArray(50)
                sr.nextBytes(code)
                oauthVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(code)

                saveOauth(oauthVerifier)

                Log.d("PKCE Log", "Code verifier = $oauthVerifier")
                //probably bad way to still get this when onResume()
                
                //Note: state is recommended, idk what it does
                val authString = "https://myanimelist.net/v1/oauth2/authorize?" +
                        "response_type=code&" +
                        "client_id=$oauthClientId&" +
                        "code_challenge=$oauthVerifier"

                val stringRequest = StringRequest(
                    Request.Method.GET,
                    authString,
                    {
                        //Change 'it' to to 'response->', because I intend to create a web-view
                        val i = Intent(Intent.ACTION_VIEW)
                        i.data = Uri.parse(authString)
                        startActivity(i)
                        //What happens next is in onResume()
                    },
                    { error ->
                        Toast.makeText(this, "ERROR: $error", Toast.LENGTH_LONG).show()
                    }
                )
                queue.add(stringRequest)

            } catch (ex: UnsupportedEncodingException) {
                Log.d("PKCE ERROR", "UnsupportedEncodingException")
            } catch (ex: NoSuchAlgorithmException) {
                Log.d("PKCE ERROR", "NoSuchAlgorithmException")
            }
        }
        binding.btnWatching.setOnClickListener {
            animeList.clear()
            accessToken = recoverToken()
            val url = "https://api.myanimelist.net/v2/users/@me/animelist?status=watching&limit=1000&nsfw=true"
            Log.d("btnWatching ACCESS_TOKEN", accessToken)

            val request: StringRequest = @SuppressLint("NotifyDataSetChanged")
            object : StringRequest(
                Method.GET, url,
                { response ->
                    if (response != null) {
                        Log.d("btnWatching SUCCESS", response)
                        try {
                            val arr = JSONObject(response).getJSONArray("data")
                            Log.d("DATA", arr.toString())
                            for (i in 0 until arr.length()) {
                                val data: JSONObject = (arr[i] as JSONObject).getJSONObject("node")
                                val id = data.getString("id").toInt()
                                val name = data.getString("title")
                                val imgURL = data.getJSONObject("main_picture").getString("large")
                                Log.d("TEST", "Data: $id, $name, $imgURL")
                                animeList.add(Anime(id, name, imgURL))
                                animeAdapter.notifyDataSetChanged()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        Log.e("btnWatching ERROR", "Data Null")
                    }
                },
                { error ->
                    Log.e("btnWatching ERROR", error.toString())
                }
            ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers: MutableMap<String, String> = HashMap()
                    headers["Authorization"] = "Bearer $accessToken"
                    return headers
                }
            }
            queue.add(request)
        }
        binding.btnGetByName.setOnClickListener {
            animeList.clear()
            val text = binding.etMain.text
            val url = "https://api.myanimelist.net/v2/anime?q=$text&nsfw=true&limit=100"
            val request: StringRequest = @SuppressLint("NotifyDataSetChanged")
            object : StringRequest(
                Method.GET, url,
                { response ->
                    if (response != null) {
                        Log.d("btnGetByName SUCCESS", response)
                        try {
                            val arr = JSONObject(response).getJSONArray("data")
                            Log.d("DATA", arr.toString())
                            for (i in 0 until arr.length()) {
                                val data: JSONObject = (arr[i] as JSONObject).getJSONObject("node")
                                val id = data.getString("id").toInt()
                                val name = data.getString("title")
                                val imgURL = data.getJSONObject("main_picture").getString("large")
                                Log.d("TEST", "Data: $id, $name, $imgURL")
                                animeList.add(Anime(id, name, imgURL))
                                animeAdapter.notifyDataSetChanged()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        Log.e("btnGetByName ERROR", "Data Null")
                    }
                },
                { error ->
                    Log.e("btnGetByName ERROR", error.toString())
                }
            ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers: MutableMap<String, String> = HashMap()
                    headers["X-MAL-CLIENT-ID"] = oauthClientId
                    return headers
                }
            }
            queue.add(request)
        }
    }

    override fun onResume() {
        super.onResume()
        oauthVerifier = recoverVerifier()
        Log.d("PKCE Log 2", "Code verifier = $oauthVerifier")

        val uri = intent.data
        if (uri != null && uri.toString().startsWith("oauth2test://callback")) {
            val oauthCode = uri.getQueryParameter("code")
            Log.d("OAuth Client Id", oauthClientId)
            Log.d("OAuth Code", "$oauthCode")
            Log.d("OAuth verifier", oauthVerifier)

            val url = "https://myanimelist.net/v1/oauth2/token"

            val queue = Volley.newRequestQueue(this)

            val request: StringRequest = object : StringRequest(
                Method.POST, url,
                Response.Listener { response ->
                    Log.d("SUCCESS", response)
                    try {
                        val jObject = JSONObject(response)
                        Log.d("TEST", jObject.getString("access_token"))
                        accessToken = jObject.getString("access_token")
                        saveToken(jObject.getString("access_token"))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }, Response.ErrorListener { error ->
                    Log.d("POST ERROR", "$error")
                    Toast.makeText(this, "OAUTH2.0 FAILURE", Toast.LENGTH_SHORT).show()
                }) {
                override fun getBodyContentType(): String {
                    return "application/x-www-form-urlencoded; charset=UTF-8"
                }
                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["client_id"] = oauthClientId
                    params["code"] = oauthCode.toString()
                    params["code_verifier"] = oauthVerifier
                    params["grant_type"] = "authorization_code"

                    return params
                }
            }
            queue.add(request)
        }
    }

    private fun saveOauth(verifier: String) {
        val save = applicationContext.getSharedPreferences("oauth", 0)
        save.edit().also {
            it.putString("verifier", verifier)
            it.apply()
        }
    }
    private fun recoverVerifier(): String {
        val save = applicationContext.getSharedPreferences("oauth", 0)
        return save.getString("verifier", "")!!
    }
    private fun saveToken(token: String) {
        val save = applicationContext.getSharedPreferences("oauth", 0)
        save.edit().also {
            it.putString("token", token)
            it.apply()
        }
    }
    private fun recoverToken(): String {
        val save = applicationContext.getSharedPreferences("oauth", 0)
        return save.getString("token", "")!!
    }
}
