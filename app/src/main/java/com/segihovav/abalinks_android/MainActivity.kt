package com.segihovav.abalinks_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.internal.ContextUtils.getActivity
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import kotlin.collections.ArrayList

// TO DO

// swipe icon disappears sometimes
// See if you can figure out a way to not exit the app after togglling dark mode in settings

class MainActivity : AppCompatActivity(), OnRefreshListener, AdapterView.OnItemSelectedListener {
    private lateinit var abaLinksURL: String
    private val abaLinksList: MutableList<AbaLink> = ArrayList()
    private var abaLinksTypes: ArrayList<AbaLinkType> = ArrayList()
    private val abaLinksTypeNames: java.util.ArrayList<String> = java.util.ArrayList()
    private lateinit var searchView: EditText
    private lateinit var swipeController: SwipeController
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var searchTypeIDSpinner: Spinner
    private lateinit var episodeListView: RecyclerView
    private val darkMode = R.style.Theme_AppCompat_DayNight
    private val lightMode = R.style.ThemeOverlay_MaterialComponents

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        this.setTheme(if (sharedPreferences.getBoolean("DarkThemeOn", false)) darkMode else lightMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set context which is used by SwipeController
        context = applicationContext

        // Internet connection is always required
        if (!isNetworkAvailable(this))
            alert("No Internet connection detected. Internet access is needed to use this app.", true)

        // Init the SwipeController
        val mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)
        swipeController = SwipeController(object : SwipeControllerActions() {}, abaLinksList)
        swipeController.setMainActivity(this)

        if (sharedPreferences.getBoolean("DarkThemeOn", false))
                swipeController.setDarkMode(true)

        // init swipe listener
        mSwipeRefreshLayout.setOnRefreshListener(this)
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, android.R.color.holo_green_dark, android.R.color.holo_orange_dark, android.R.color.holo_blue_dark)
        mSwipeRefreshLayout.setOnRefreshListener { loadJSONData(true); searchView.text = searchView.text }

        episodeListView = findViewById(R.id.episodeList)

        abaLinksURL = if (sharedPreferences.getString("AbaLinksURL", "") != null) sharedPreferences.getString("AbaLinksURL", "").toString() else ""

        // Make sure that abaLinksURL always ends in a black slash
        if (abaLinksURL != "" && !abaLinksURL.endsWith("/"))
            abaLinksURL+="/"

        if (abaLinksURL == "") {
            loadSettingsActivity()
        }

        searchView = findViewById(R.id.searchView)

        searchTypeIDSpinner = findViewById<Spinner>(R.id.searchTypeIDSpinner)

        searchTypeIDSpinner.onItemSelectedListener = this

        // Search change event
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    val abaLinksListFiltered: MutableList<AbaLink> = ArrayList()

                    for (i in abaLinksList.indices) {
                        // If the search term is contained in the name or URL
                        val itemName = abaLinksList[i].Name?.toLowerCase(Locale.ROOT)
                        val itemURL = abaLinksList[i].URL
                        val itemTypeID = abaLinksList[i].TypeID

                        val searchTerm = s.toString().toLowerCase(Locale.ROOT)
                        var searchTypeID = -1

                        for (j in abaLinksTypes.indices) {
                            if (abaLinksTypes[j].Name == searchTypeIDSpinner.selectedItem)
                                searchTypeID = abaLinksTypes[j].ID
                        }

                        when (searchTerm) {
                            "" -> if ((searchTypeID == -1 || searchTypeID == 6) || (searchTypeID != -1 && searchTypeID == itemTypeID)) {
                                abaLinksListFiltered.add(abaLinksList[i])
                            }
                            else -> {
                                if (itemName != null && itemName.contains(searchTerm) && ((searchTypeID == -1 || searchTypeID == 6) || (searchTypeID != -1 && searchTypeID == itemTypeID))) {
                                    abaLinksListFiltered.add(abaLinksList[i])
                                } else if (itemURL != null && itemURL.contains(s) && ((searchTypeID == -1 || searchTypeID == 6) || (searchTypeID != -1 && searchTypeID == itemTypeID))) {
                                    abaLinksListFiltered.add(abaLinksList[i])
                                }
                            }
                        }
                    }

                    // Call method that reloads the recycler view with the current data
                    initRecyclerView(abaLinksListFiltered)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)

        // Search search menu icon based on the current theme
        searchMenuItem.setIcon(R.drawable.search_white)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
       super.onConfigurationChanged(newConfig)

       if (episodeListView.adapter != null) episodeListView.adapter?.notifyDataSetChanged()
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        // Set text to itself if the user selects a type filter to trigger the searchView.addTextChangedListener event
        searchView.text = searchView.text
    }

    override fun onNothingSelected(p0: AdapterView<*>?) { }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) { // Settings menu
            loadSettingsActivity()

            return true
        } else if (id == R.id.action_add) {
            val intent = Intent(this, EditActivity::class.java)

            intent.putExtra(getApplicationContext().getPackageName() + ".IsAdding", true)
            intent.putExtra(getApplicationContext().getPackageName() + ".LinkTypes",abaLinksTypes)
            intent.putExtra(getApplicationContext().getPackageName() + ".LinkTypeNames",abaLinksTypeNames)

            startActivity(intent)
        } else if (id == R.id.action_search) {
            searchViewIsVisible()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() { }

    // Fixes the issue that causes the swipe buttons to disappear when leaving the app
    public override fun onResume() {
        super.onResume()

        val extras = intent.extras

        if (extras != null && extras.getBoolean(getApplicationContext().getPackageName() + ".DarkModeToggled")) {
            //val intent = Intent(this, MainActivity::class.java)

            //startActivity(intent)

            finishAndRemoveTask()

            //recreate()

            System.exit(0);

            return
        }

        // When resuming this activity, hide the search field and search type dropdown
        //if (searchView != null && searchView.visibility == View.VISIBLE)
        //    searchViewIsVisible()

        if (abaLinksURL != "")
             loadTypes()

        if (episodeListView.adapter != null) episodeListView.adapter?.notifyDataSetChanged()
    }

    // Event when this activity returns from another activity
    public override fun onStart() {
        super.onStart()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Since onCreate() doesn't get called when returning from another activity, we have to set AbaLinksURL here
        abaLinksURL = if (sharedPreferences.getString("AbaLinksURL", "") != null) sharedPreferences.getString("AbaLinksURL", "").toString() else ""

        if (abaLinksURL != "" && !abaLinksURL.endsWith("/"))
            abaLinksURL+="/"

        if (abaLinksTypes.size == 0 && abaLinksURL != "")
             loadTypes()
    }

    public override fun onStop() {
        super.onStop()
    }

    private fun alert(message: String, closeApp: Boolean = false) {
        // Display dialog
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message).setCancelable(false)
               .setPositiveButton("OK") { _, _ -> if (closeApp) finish() }

        val alert = builder.create()

        alert.show()
    }

    private fun initRecyclerView(arrayList: List<AbaLink>) {
        val abaLinkNames: MutableList<String>
        val abaLinkTypeNames: MutableList<String> = ArrayList() // Used to save the typw
        val adapter: AbaLinksAdapter
        val layoutManager: RecyclerView.LayoutManager
        val mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)

        // Creates names array used as the item label for the RecyclerView
        abaLinkNames = ArrayList()
        abaLinkNames.clear()

        for (i in arrayList.indices) {
            abaLinkNames.add("<A HREF='" + arrayList[i].URL + "'>" + arrayList[i].Name + "</A>")

            // Type names
            for (j in abaLinksTypes.indices) {
                if (abaLinksTypes[j].ID==arrayList[i].TypeID && abaLinksTypes[j].Name != "") {
                    abaLinkTypeNames.add(abaLinksTypes[j].Name.toString())
                }
            }
        }

        // specify an adapter
        adapter = AbaLinksAdapter(abaLinkNames, abaLinkTypeNames)
        adapter.notifyDataSetChanged()

        layoutManager = LinearLayoutManager(applicationContext)

        episodeListView.layoutManager = layoutManager
        episodeListView.itemAnimator = DefaultItemAnimator()

        swipeController.setAbaLinksList(arrayList)

        val itemTouchhelper = ItemTouchHelper(swipeController)
        itemTouchhelper.attachToRecyclerView(episodeListView)

        episodeListView.adapter = adapter
        registerForContextMenu(episodeListView)

        mSwipeRefreshLayout.isRefreshing = false
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) != null
        } catch (e: Exception) {
            true
        }
    }

    private fun loadJSONData(isRefreshing: Boolean = false) {
        val getLinkDataEndpoint = "LinkData.php?task=fetchData"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val request = JsonArrayRequest(
                Request.Method.GET,
                abaLinksURL + getLinkDataEndpoint,
                null,
                Response.Listener { response ->
                    lateinit var jsonarray: JSONArray
                    try {
                        jsonarray = JSONArray(response.toString())
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    // This is needed so that when the user pulls to refresh, all previous items are removed to avoid duplicates
                    abaLinksList.clear()

                    for (i in 0 until jsonarray.length()) {
                        try {
                            val jsonobject = jsonarray.getJSONObject(i)

                            abaLinksList.add(AbaLink(jsonobject.getString("ID").toInt(), jsonobject.getString("Name"), jsonobject.getString("URL"), jsonobject.getString("TypeID").toInt()))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            alert("An error occurred reading the links. Please check your network connection", true)
                        }
                    }

                    if (!isRefreshing)
                        initRecyclerView(abaLinksList)
                },
                Response.ErrorListener {
                    //System.out.println("****** Error response=" + error.toString());
                    alert("An error occurred reading the links. Please check your network connection", true)
                })
        requestQueue.add(request)
    }

    private fun loadTypes() {
         val getLinkDataEndpoint = "LinkData.php?task=fetchTypes"
         val requestQueue: RequestQueue = Volley.newRequestQueue(this)
         val request = JsonArrayRequest(
                 Request.Method.GET,
                 abaLinksURL + getLinkDataEndpoint,
                 null,
                 Response.Listener { response ->
                     lateinit var jsonarray: JSONArray

                     try {
                         jsonarray = JSONArray(response.toString())
                     } catch (e: JSONException) {
                         e.printStackTrace()
                     }

                     abaLinksTypes.clear()
                     abaLinksTypeNames.clear()

                     for (i in 0 until jsonarray.length()) {
                         try {
                             val jsonobject = jsonarray.getJSONObject(i)

                             abaLinksTypes.add(AbaLinkType(jsonobject.getInt("id"), jsonobject.getString("value")))

                             abaLinksTypeNames.add(jsonobject.getString("value"))
                         } catch (e: JSONException) {
                             e.printStackTrace()
                         }
                     }

                     // Creating adapter for spinner
                     val dataAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, abaLinksTypeNames)

                     // attaching data adapter to spinner
                     searchTypeIDSpinner.adapter = dataAdapter

                     swipeController.setLinkTypes(abaLinksTypes)

                     swipeController.setLinkTypeNames(abaLinksTypeNames)

                     loadJSONData()
                 },
                 Response.ErrorListener {
                     alert("An error occurred reading the types. Please check your network connection", true)
                 })
        requestQueue.add(request)
    }

    private fun loadSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    // Visibility is always toggled
    fun searchViewIsVisible() {
        val swipeControl = findViewById<SwipeRefreshLayout>(R.id.swipe_container)

        val isHidden= searchView.visibility != View.VISIBLE

        searchView.visibility = if (!isHidden) View.GONE else View.VISIBLE
        searchView.requestFocus()

        searchTypeIDSpinner.visibility = if (!isHidden) View.GONE else View.VISIBLE

        // Set top margin on tthe swipeControl which contains the episode Recyclerview when showing the search field and search type dropdown
        val params: ViewGroup.MarginLayoutParams = swipeControl.layoutParams as ViewGroup.MarginLayoutParams

        if (isHidden) {
            params.setMargins(0, 300, 0, 0)
        } else {
            params.setMargins(0, 0, 0, 0)
        }

        swipeControl.layoutParams=params
    }

    // Supresses warning about the class property
    companion object {
        @JvmField
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }
}
