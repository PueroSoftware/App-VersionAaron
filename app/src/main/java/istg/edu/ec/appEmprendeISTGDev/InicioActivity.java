package istg.edu.ec.appEmprendeISTGDev;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import istg.edu.ec.appEmprendeISTGDev.data.model.UserModel;
import istg.edu.ec.appEmprendeISTGDev.databinding.ActivityInicioBinding;
import istg.edu.ec.appEmprendeISTGDev.viewModel.PermisosViewModel;
import istg.edu.ec.appEmprendeISTGDev.viewModel.UserViewModel;
import static istg.edu.ec.appEmprendeISTGDev.utils.StatusBarUtilsKt.setStatusBarColor;
import istg.edu.ec.appEmprendeISTGDev.utils.DeepLinkManager;

public class InicioActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityInicioBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressDialog progressDialog;
    private UserViewModel userViewModel;
    private PermisosViewModel permisosViewModel;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private NavigationView navigationView;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStatusBarColor(this, R.color.azul_navbar, true); // Fondo azul, íconos negros

        // --- Inyección de vistas y ViewModels ---
        sharedPreferences = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        permisosViewModel = new ViewModelProvider(this).get(PermisosViewModel.class);

        binding = ActivityInicioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Firebase Auth ---
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // --- NavigationView ---
        navigationView = binding.navView;

        if (currentUser != null) {
            String uid = currentUser.getUid();
            editor.putString("uid", uid).apply();

            // Verificar si el usuario es administrador
            permisosViewModel.checkAdminStatus(uid);
            permisosViewModel.getEsAdmin().observe(this, esAdmin -> {
                Menu menu = navigationView.getMenu();
                menu.findItem(R.id.opcionesAdministradorFragment)
                        .setVisible(esAdmin != null && esAdmin);
                Log.d("InicioActivity", "Es admin: " + esAdmin);
            });

            View headerView = navigationView.getHeaderView(0);
            TextView tvEmail = headerView.findViewById(R.id.textView);
            tvEmail.setText(currentUser.getEmail());

            userViewModel.consultarUsuario(currentUser.getEmail());
        } else {
            Log.e("InicioActivity", "Usuario no autenticado");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // --- Toolbar & Drawer ---
        setSupportActionBar(binding.appBarInicio.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.perfilFragment,
                R.id.nav_home,
                R.id.misNegociosFragment,
                R.id.filtroBusquedaFragment,
                R.id.opcionesAdministradorFragment
        )
                .setOpenableLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_inicio);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                NavDestination dest = navController.getCurrentDestination();
                int idToCheck = (dest != null) ? dest.getId() : R.id.nav_home;
                navigationView.setCheckedItem(idToCheck);
            }
        });

        // --- Google Sign-In ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- ProgressDialog ---
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Cerrando sesión...");
        progressDialog.setCancelable(false);

        // --- Observador de Usuario Firestore ---
        userViewModel.getUsuario().observe(this, usuario -> {
            if (usuario == null) {
                guardarUsuario(currentUser);
            } else {
                editor.putString("uid", usuario.getUid()).apply();
            }
        });

        // --- Navigation sin "saltito" ---
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            boolean handled = NavigationUI.onNavDestinationSelected(menuItem, navController);

            if (menuItem.getItemId() == R.id.filtroBusquedaFragment) {
                navController.navigate(R.id.filtroBusquedaFragment);
                handled = true;
            }

            if (menuItem.getItemId() == R.id.nav_share) {
                compartirApp();
                handled = true;
            }

            if (handled) {
                drawer.closeDrawers();
            }
            return handled;
        });

        // --- Manejo de Deep Link ---
        // Procesa el intent que inició la actividad
        DeepLinkManager.INSTANCE.handleIncomingIntent(
                this,
                getIntent(),
                R.id.nav_host_fragment_content_inicio, // ID del NavHost
                null, // ID del destino (null para que NavController use el deep link del nav graph)
                "userId"
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Actualiza el intent de la actividad
        // Procesa el nuevo intent si la actividad ya estaba abierta
        DeepLinkManager.INSTANCE.handleIncomingIntent(
                this,
                intent,
                R.id.nav_host_fragment_content_inicio,
                null,
                "userId"
        );
    }

    private void guardarUsuario(FirebaseUser fireuser) {
        if (fireuser == null) return;
        UserModel user = new UserModel(fireuser.getEmail(), fireuser.getDisplayName(), fireuser.getUid());
        userViewModel.saveUsuario(user);
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void compartirApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "👋 Hola, te invito a descargar *App Emprende ISTG* 🚀 \n" +
                        "👉 Descárgala aquí: https://play.google.com/store/apps/details?id=istg.edu.ec.appEmprendeISTGDev"
        );
        startActivity(Intent.createChooser(shareIntent, "Compartir vía"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inicio, menu);
        MenuItem item = menu.findItem(R.id.action_settings);
        SpannableString spanString = new SpannableString(item.getTitle());
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        int color = isNight ? Color.WHITE : Color.BLACK;
        spanString.setSpan(new ForegroundColorSpan(color), 0, spanString.length(), 0);
        item.setTitle(spanString);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        progressDialog.show();
                        signOut();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
