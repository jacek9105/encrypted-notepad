package pl.notatnik;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

//Pierwsze Activity widoczne w aplikacji, trzeba w nim wpisać hasło, aby przejść dalej.
//W różnych miejscach jest używane runOnUiThread - jest to spowodowane tym, że biblioteka szyfrująca
//zwraca wynik asynchronicznie w osobnym wątku, a dokonać operacji związanych z widokiem (np. wyświetlenie Toast) można wykonać jedynie z wątku głównego UI.
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private DataHandler dataHandler;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataHandler = new DataHandler(this);

        editTextPassword = findViewById(R.id.editTextPassword);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        //jeśli hasło nie jest utworzone to należy zapisać w pamięci wprowadzone hasło
        if (!dataHandler.isPasswordCreated()) {
            if(editTextPassword.getText().length() > 0) {
                dataHandler.createPassword(editTextPassword.getText().toString(), new SaveListener() {
                    @Override
                    public void onSaveSuccess() {
                        openNotepad();
                    }

                    @Override
                    public void onSaveError() {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błąd podczas zapisu hasła.", Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                Toast.makeText(this, "Wprowadź hasło.", Toast.LENGTH_SHORT).show();
            }
        } else {
            //jeśli hasło jest utworzone to należy sprawdzić czy wprowadzone jest zgodne
            dataHandler.checkPassword(editTextPassword.getText().toString(), checked -> {
                if(checked)
                    openNotepad();
                else
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błędne hasło.", Toast.LENGTH_SHORT).show());
            });
        }
    }

    private void openNotepad() {
        runOnUiThread(() -> {
            //otwieramy nowe Activity i przekazujemy do niego wprowadzone hasło dla dodatkowego zabezpieczenia dostępu
            Intent intent = new Intent(this, NotepadActivity.class);
            intent.putExtra(NotepadActivity.PASSWORD, editTextPassword.getText().toString());
            startActivity(intent);
            finish();
        });
    }
}
