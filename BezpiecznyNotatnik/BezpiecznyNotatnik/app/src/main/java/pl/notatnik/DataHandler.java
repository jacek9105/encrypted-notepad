package pl.notatnik;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.pvryan.easycrypt.ECResultListener;
import com.pvryan.easycrypt.hash.ECHash;
import com.pvryan.easycrypt.hash.ECHashAlgorithms;
import com.pvryan.easycrypt.symmetric.ECSymmetric;

import org.jetbrains.annotations.NotNull;

//Klasa z metodami związanymi z działaniem na danych tj. na haśle i notatce.
//Zawiera metody które używają zewnętrznej biblioteki EasyCrypt https://github.com/pvasa/EasyCrypt
//Odpowiada ona za działania związane z szyfrowaniem i odszyfrowaniem notatki oraz z hashowaniem hasła.
//Biblioteka działa asynchronicznie stąd są utworzone listenery, które zwracają wynik działania biblioteki.
public class DataHandler {

    private static final String KEY_PASSWORD = "key_password";
    private static final String KEY_NOTE = "key_note";

    private SharedPreferences sharedPref;

    public DataHandler(Activity activity) {
        //Dane zapisujemy w SharedPreferences w trybie private, a więc tylko ta aplikacja ma dostęp do tych danych.
        //To dodatkowe zabezpieczenie oprócz szyfrowania i hashowania.
        sharedPref = activity.getSharedPreferences("pl.notatnik", Context.MODE_PRIVATE);
    }

    //Metoda sprawdza czy hasło jest stworzone i zapisane w pamieci. Jeśli nie jest to zostanie utworzone.
    //Jeśli jest to wprowadzone hasło będize porównywane z istniejącym.
    public boolean isPasswordCreated() {
        return sharedPref.getString(KEY_PASSWORD, null) != null;
    }

    //Metoda zapisuje nowe hasło.
    //Najpierw je hashuje używając algorytmu SHA-1, a następnie zapisuje w SharedPreferences oraz wywołuje listener.
    public void createPassword(String password, SaveListener listener) {
        ECHash eCryptHash = new ECHash();
        eCryptHash.calculate(password, ECHashAlgorithms.SHA_1, new ECResultListener() {
            @Override
            public void onProgress(int i, long l, long l1) {
            }

            @Override
            public <T> void onSuccess(T result) {
                //result to zahashowane hasło, możemy je zapisać w pamięci
                sharedPref.edit().putString(KEY_PASSWORD, result.toString()).apply();
                listener.onSaveSuccess();
            }

            @Override
            public void onFailure(@NotNull String s, @NotNull Exception e) {
                listener.onSaveError();
            }
        });
    }

    //Metoda sprawdza poprawność hasła z zapisanym w SharedPreferences. Najpierw je hashuje, następnie pobiera zapisane i porównuje.
    public void checkPassword(String password, PasswordCheckListener listener) {
        if (password == null) {
            listener.onPasswordChecked(false);
            return;
        }
        ECHash eCryptHash = new ECHash();
        eCryptHash.calculate(password, ECHashAlgorithms.SHA_1, new ECResultListener() {
            @Override
            public void onProgress(int i, long l, long l1) {
            }

            @Override
            public <T> void onSuccess(T result) {
                //result to zahashowane hasło wprowadzone przez użytkownika, możemy je porównać z zahashowanym hasłem zapisanym w pamięci.
                String saved = sharedPref.getString(KEY_PASSWORD, null);
                listener.onPasswordChecked(result.equals(saved));
            }

            @Override
            public void onFailure(@NotNull String s, @NotNull Exception e) {
                listener.onPasswordChecked(false);
            }
        });
    }

    //Przy zmianie hasła musimy odczytać dotychczasową notatkę używając starego hasła,
    //następnie zapisać ją używając nowego hasła i dopiero zapisać nowe hasło.
    //W przeciwnym wypadku jej odczyt byłby już niemożliwy.
    public void changePassword(String oldPassword, String newPassword, SaveListener listener) {
        getNote(oldPassword, new ReadNoteListener() {
            @Override
            public void onReadFinished(String note) {
                saveNote(newPassword, note, new SaveListener() {
                    @Override
                    public void onSaveSuccess() {
                        createPassword(newPassword, listener);
                    }

                    @Override
                    public void onSaveError() {
                        listener.onSaveError();
                    }
                });
            }

            @Override
            public void onReadError() {
                listener.onSaveError();
            }
        });
    }

    //Metoda pobiera notatkę z pamięci. Odszyfrowuje ją i zwraca w listenerze.
    public void getNote(String password, ReadNoteListener listener) {
        ECSymmetric eCryptSymmetric = new ECSymmetric();
        String note = sharedPref.getString(KEY_NOTE, "");
        if (note.length() == 0) {
            listener.onReadFinished("");
        } else {
            eCryptSymmetric.decrypt(note, password, new ECResultListener() {
                @Override
                public void onProgress(int i, long l, long l1) { }

                @Override
                public <T> void onSuccess(T result) {
                    //result to odszyfrowana notatka, można ją zwrócić w listenerze
                    listener.onReadFinished(result.toString());
                }

                @Override
                public void onFailure(@NotNull String s, @NotNull Exception e) {
                    e.printStackTrace();
                    listener.onReadError();
                }
            });
        }
    }

    //Metoda zapisuje notatkę. Najpierw ją szyfruje i następnie zapisuje w pamięci.
    //Do szyfrowania używany jest symetryczny algorytm AES256.
    public void saveNote(String password, String note, SaveListener listener) {
        if (note.length() == 0) {
            sharedPref.edit().putString(KEY_NOTE, "").apply();
            listener.onSaveSuccess();
        } else {
            ECSymmetric eCryptSymmetric = new ECSymmetric();
            eCryptSymmetric.encrypt(note, password, new ECResultListener() {
                @Override
                public void onProgress(int i, long l, long l1) {
                }

                @Override
                public <T> void onSuccess(T result) {
                    sharedPref.edit().putString(KEY_NOTE, result.toString()).apply();
                    listener.onSaveSuccess();
                }

                @Override
                public void onFailure(@NotNull String s, @NotNull Exception e) {
                    listener.onSaveError();
                }
            });
        }
    }

    //Metoda usuwa z pamięci zapisane dane tj. hasło i notatkę.
    public void resetData() {
        sharedPref.edit().clear().apply();
    }

}
