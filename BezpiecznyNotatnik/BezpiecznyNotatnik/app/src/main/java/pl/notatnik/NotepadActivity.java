package pl.notatnik;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NotepadActivity extends AppCompatActivity {

    public static final String PASSWORD = "password";

    private DataHandler dataHandler;
    private String password;
    private EditText editTextNewPassword;
    private EditText editTextNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notepad);

        dataHandler = new DataHandler(this);

        //Dodatkowe zabezpieczenie przed dostępem, do tego Activity trzeba przekazać hasło, które jest jeszcze raz weryfikowane.
        //W przypadku braku przekazanego hasła lub gdy jest ono niepoprawne Activity jest zamykane i otwierane ponownie MainActivity do wpisania hasła.
        if (getIntent() != null) {
            password = getIntent().getStringExtra(PASSWORD);
            dataHandler.checkPassword(password, checked -> {
                if (checked)
                    init();
                else
                    closeAndOpenMainActivity();
            });
        } else {
            closeAndOpenMainActivity();
        }
    }

    private void init() {
        editTextNote = findViewById(R.id.editTextNote);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);

        findViewById(R.id.buttonSaveNote).setOnClickListener(view -> dataHandler.saveNote(password, editTextNote.getText().toString(), new SaveListener() {
            @Override
            public void onSaveSuccess() {
                runOnUiThread(() -> Toast.makeText(NotepadActivity.this, "Zapisano notatkę.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onSaveError() {
                runOnUiThread(() -> Toast.makeText(NotepadActivity.this, "Błąd podczas zapisu.", Toast.LENGTH_SHORT).show());
            }
        }));

        findViewById(R.id.buttonChangePassword).setOnClickListener(view -> {
            if (editTextNewPassword.getText().length() > 0) {
                dataHandler.changePassword(password, editTextNewPassword.getText().toString(), new SaveListener() {
                    @Override
                    public void onSaveSuccess() {
                        runOnUiThread(() -> Toast.makeText(NotepadActivity.this, "Hasło zostało zmienione.", Toast.LENGTH_SHORT).show());
                        password = editTextNewPassword.getText().toString();
                    }

                    @Override
                    public void onSaveError() {
                        runOnUiThread(() -> Toast.makeText(NotepadActivity.this, "Błąd podczas zmiany hasła.", Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                Toast.makeText(NotepadActivity.this, "Wprowadź nowe hasło, aby je zmienić.", Toast.LENGTH_SHORT).show();
            }
        });

        //po kliknięciu na reset aplikacji wyświetlamy okienko z pytaniem czy na pewno usunąć i jeśli użytkownik potwierdzi to wykonujemy operację.
        //okienko dialogowe jest zamykane, usuwamy z pamięci zapisane dane, wyświetlamy informację o dokonaniu resetu i zamykamy obecne Activity i otwieramy MainActivity
        findViewById(R.id.buttonResetApp).setOnClickListener(view -> new AlertDialog.Builder(NotepadActivity.this)
                .setMessage("Czy jesteś pewien, że chcesz zresetować aplikację? Zapisana notatka zostanie usunięta.")
                .setPositiveButton("Tak", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    dataHandler.resetData();
                    Toast.makeText(NotepadActivity.this, "Aplikacja została zresetowana i dane usunięte.", Toast.LENGTH_SHORT).show();
                    closeAndOpenMainActivity();
                })
                .setNegativeButton("Nie", (dialogInterface, i) -> dialogInterface.dismiss())
                .show()
        );

        //pobierz notatkę z pamięci i wyświetl ją w polu tekstowym
        dataHandler.getNote(password, new ReadNoteListener() {
            @Override
            public void onReadFinished(String note) {
                runOnUiThread(() -> editTextNote.setText(note));
            }

            @Override
            public void onReadError() {
                runOnUiThread(() -> Toast.makeText(NotepadActivity.this, "Błąd podczas odczytu notatki.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void closeAndOpenMainActivity() {
        //otwórz MainActivity i zamknij obecne
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
