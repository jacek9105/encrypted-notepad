package pl.notatnik;

public interface ReadNoteListener {
    void onReadFinished(String note);
    void onReadError();
}
