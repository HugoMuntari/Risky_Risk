package Controller;


import Model.Joueur;
import Model.Partie;
import View.Game_View;
import View.Menu_View;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Created by yhaffner on 21/11/16.
 */
public class Control_Menu implements EventHandler<ActionEvent> {
    private Menu_View view;

    public Control_Menu(Menu_View menu) {
        this.view = menu;
        this.view.setController(this);

        // En attendant un menu fonctionnel, pour passer direct au jeu:
         Partie p = new Partie();
         p.ajouterJoueur(new Joueur("Pierre",0));
         p.ajouterJoueur(new Joueur("Gérard",1));
         new Control_Game(p,this);
        // fin truc inutile
    }

    @Override
    // TODO ULTRA IMPORTANT
    public void handle(ActionEvent event) {
        /*
        Réception des objets du genre:

        si(event.getSource().equals(view.bouton){
            traiter...
            ex: view.setOptionsView();
        }

        pour le début de partie, il faudra juste créer ça:
                Partie p = new Partie();
                ajouter à p les joueurs
                new Control_Game(p,this);
       c tout!
         */
    }

    public Menu_View getView() {
        return view;
    }
}
