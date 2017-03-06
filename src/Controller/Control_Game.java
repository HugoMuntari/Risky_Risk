package Controller;


import Model.Case;
import Model.Joueur;
import Model.Partie;
import View.Game_View;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Path;
import java.io.File;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by yhaffner on 15/12/16.
 */
public class Control_Game implements EventHandler<MouseEvent>{
    public final static Random loto = new Random();
    private final Partie model;
    private final Game_View view;
    private final Control_Menu menu;
    private final AudioClip clip;
    private Timer tourTimeOut;
    private boolean[] isMoving = new boolean[4]; // DIRECTION selon le sens horaire, comme en CSS

    public Control_Game(Partie model,Control_Menu control_menu, boolean nouvelle){
        this.model=model;
        model.setJoueurCourant(loto.nextInt(model.getJoueurs().size()));
        if (nouvelle) {
            for (Joueur j : model.getJoueurs())
                model.conquerirNeutre(j, model.getNeutres().get(loto.nextInt(model.getNeutres().size())), 1);
        }
        this.view = new Game_View(model,control_menu.getView().getStage());
        this.menu = control_menu;


        view.endTurn.setText("Placement renforts auto");
        model.calculRenforts(model.getJoueurCourant());
        view.notice.setText(model.getJoueurCourant().getNom()+"\nPlacez vos renforts!");
        view.bouton_volume.setImage(new Image(new File("img/sound_"+(model.mute?"off":"on")+".png").toURI().toString()));
        if(model.getMode()==Partie.CLASSICO)
            view.mode.setText("Partie Classique");
        else
            view.mode.setText("Partie Rapide");

        setEvenHandlers();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(isMoving[0]){
                    model.map_translate.y -= view.stage.getScene().getHeight()/75;
                    view.actualiserAffichage();
                } else if (isMoving[1]) {
                    model.map_translate.x += view.stage.getScene().getWidth()/125;
                    view.actualiserAffichage();
                } else if (isMoving[2]) {
                    model.map_translate.y += view.stage.getScene().getHeight()/75;
                    view.actualiserAffichage();
                } else if (isMoving[3]) {
                    model.map_translate.x -= view.stage.getScene().getWidth()/125;
                    view.actualiserAffichage();
                }
            }
        },10,50);

        File file = new File("musics/RiskSoundtrack.wav");
        clip = new AudioClip(file.toURI().toString());
        clip.setVolume(control_menu.musicVolume);
        clip.setCycleCount(AudioClip.INDEFINITE);
        clip.play();
        model.time_secondes=System.currentTimeMillis();
        tourTimeOut = new Timer();
        tourTimeOut.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater((()->view.timer.setText(
                        (int)((model.time_secondes - System.currentTimeMillis()) / 1000 + 60) + ""
                )));
                if ((model.time_secondes - System.currentTimeMillis()) / 1000 + 60 <= 0) {
                    tourTimeOut.cancel();
                    tourTimeOut.purge();
                    model.passeJoueurSuivant();
                    model.passeEtapeSuivante();
                    model.calculRenforts(model.getJoueurCourant());
                    view.notice.setText(model.getJoueurCourant().getNom() + "\nPlacez vos renforts!");
                    view.caseOnFocus = null;
                    verifRenfortCapacite();
                }
            }
        }, 0,30);


        view.setGameView();
        view.actualiserAffichage();
    }

    private void setEvenHandlers() {
        view.setController(this);
        view.stage.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(!model.pause || event.getCode()==KeyCode.ESCAPE) {
                    switch (event.getCode()) {
                        case UP:
                            model.map_translate.y -= 70;
                            break;
                        case DOWN:
                            model.map_translate.y += 70;
                            break;
                        case LEFT:
                            model.map_translate.x -= 70;
                            break;
                        case RIGHT:
                            model.map_translate.x += 70;
                            break;
                        case ADD:
                            model.map_zoom += 0.1;
                            break;
                        case SUBTRACT:
                            model.map_zoom -= 0.1;
                            break;
                        case ESCAPE:
                            model.pause = !model.pause;
                            view.menu_pane.setVisible(model.pause);
                            break;
                    }
                    view.actualiserAffichage();
                }
            }
        });
        view.stage.getScene().setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(!model.pause) {
                    isMoving[0] = event.getY() < 15;
                    isMoving[1] = Math.abs(event.getX() - view.stage.getScene().getWidth()) < 15;
                    isMoving[2] = Math.abs(event.getY() - view.stage.getScene().getHeight()) < 15;
                    isMoving[3] = event.getX() < 15;
                }
            }
        });
        view.stage.getScene().setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if(!model.pause) {
                    model.map_zoom += event.getDeltaY() / 400;
                    view.actualiserAffichage();
                }
            }
        });
    }

    @Override
    public void handle(MouseEvent event) {
        /* SOUND */
        if (!model.mute && event.getSource() instanceof Button) {
            AudioClip clip = new AudioClip(new File("sounds/button.wav").toURI().toString());
            clip.setVolume(menu.soundVolume);
            clip.play();;
        }


        /* ACTIONS */
        boolean actualiseCases = true;
        if(event.getSource().equals(view.endTurn)) {
            if(!view.endTurn.getText().equals("Placement renforts auto")) {
                view.endTurn.setText("Placement renforts auto");
                model.passeJoueurSuivant();
                model.passeEtapeSuivante();
                model.calculRenforts(model.getJoueurCourant());
                view.notice.setText(model.getJoueurCourant().getNom() + "\nPlacez vos renforts!");
                view.caseOnFocus = null;
                verifRenfortCapacite();

                if(tourTimeOut!=null) tourTimeOut.purge();
                tourTimeOut = new Timer();
                tourTimeOut.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater((()->view.timer.setText(
                                (int)((model.time_secondes - System.currentTimeMillis()) / 1000 + 60) + ""
                        )));
                        if ((model.time_secondes - System.currentTimeMillis()) / 1000 + 60 <= 0) {
                            tourTimeOut.cancel();
                            tourTimeOut.purge();
                            model.passeJoueurSuivant();
                            model.passeEtapeSuivante();
                            model.calculRenforts(model.getJoueurCourant());
                            view.notice.setText(model.getJoueurCourant().getNom() + "\nPlacez vos renforts!");
                            view.caseOnFocus = null;
                            verifRenfortCapacite();
                        }
                    }
                }, 0,30);
            } else {
                while (model.isDistributionRenforts())
                    for (Case c : model.getJoueurCourant().getTerrain()) {
                        view.caseOnFocus = null;
                        if ((model.getMode() == Partie.CLASSICO && c.getNbtroupes() < 24) || (model.getMode() == Partie.RAPIDO && c.getNbtroupes() < 12)) {
                            c.addRenforts();
                            model.getJoueurCourant().setNbRenforts(model.getJoueurCourant().getNbRenforts() - 1);
                        }
                        if (model.getJoueurCourant().getNbRenforts() > 0) {
                            view.notice.setText(model.getJoueurCourant().getNom() + "\n" +
                                    model.getJoueurCourant().getNbRenforts() + " renfort(s) restant");
                        } else {
                            model.passeEtapeSuivante();
                            view.notice.setText(model.getJoueurCourant().getNom() + "\n" +
                                    "Cliquez sur une de vos case puis sur une case adversaire ou neutre pour tenter de la conquérir");
                            view.endTurn.setText("Terminer le tour");
                            break;
                        }
                        verifRenfortCapacite();
                    }
            }
        } else if(event.getSource() instanceof Path) {
            Path b = ((Path) event.getSource());
            Case c = view.allCases.get(b);

            if(model.isDistributionRenforts() && model.getJoueurCourant().getTerrain().contains(c)) {
                view.caseOnFocus=null;
                if ((model.getMode()== Partie.CLASSICO && c.getNbtroupes()<24) || (model.getMode()== Partie.RAPIDO && c.getNbtroupes()<12)){
                    c.addRenforts();
                    model.getJoueurCourant().setNbRenforts(model.getJoueurCourant().getNbRenforts()-1);
                }
                if(model.getJoueurCourant().getNbRenforts()>0) {
                    view.notice.setText(model.getJoueurCourant().getNom() + "\n" +
                            model.getJoueurCourant().getNbRenforts() + " renfort(s) restant");
                } else {
                    model.passeEtapeSuivante();
                    view.notice.setText(model.getJoueurCourant().getNom()+"\n"+
                            "Cliquez sur une de vos case puis sur une case adversaire ou neutre pour tenter de la conquérir");
                    view.endTurn.setText("Terminer le tour");
                }
                verifRenfortCapacite();
            } else if(model.isAttaque_deplacements()) {
                Case caseattaquante=view.allCases.get(view.caseOnFocus);
                if((view.caseOnFocus != null) && (view.allCases.get(view.caseOnFocus).getNbtroupes() > 1) && (view.allCases.get(view.caseOnFocus).getJoueur() == model.getJoueurCourant()) && model.getJoueurCourant().getTerrain().get(model.getJoueurCourant().getindexTerrain(view.allCases.get(view.caseOnFocus))).getVoisins().contains(c)) {
                    if (model.getNeutres().contains(c)) {
                        if (c.getNbtroupes() == 0) {
                            model.conquerirNeutre(model.getJoueurCourant(), c, caseattaquante.getNbtroupes()-1);
                            model.getJoueurCourant().getTerrain().get(model.getJoueurCourant().getindexTerrain(caseattaquante)).setNbtroupes(1);
                        }
                    } else if (!model.getJoueurCourant().getTerrain().contains(c)){
                        for (Joueur j : model.getJoueurs()) {
                            if (j.getTerrain().contains(c)) {
                                if(model.getMode()==Partie.CLASSICO) {
                                    model.actualAttCase = caseattaquante;
                                    model.actualDefCase = c;
                                    (new Thread(() -> {
                                        do {
                                            long time = System.currentTimeMillis();
                                            model.captureTerrainAdverse(model.getJoueurCourant(), j, model.actualDefCase, model.actualAttCase,model.actualAttCase.getNbtroupes());
                                            view.model_des.launchDices(model.getDeAttaquant(), model.getDeDefenseur(), model.getFightResult());
                                            Platform.runLater(view::actualizeCases);
                                            try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}
                                        } while (model.actualAttCase.getNbtroupes() > 1 && model.lanceContinue);
                                        model.lanceContinue = true;
                                        view.lanceContinue.setDisable(false);
                                        Platform.runLater(this::verifFinDePartie);
                                    })).start();
                                    actualiseCases = false;
                                } else
                                    model.captureTerrainAdverse(model.getJoueurCourant(), j, c, caseattaquante,caseattaquante.getNbtroupes());
                                break;
                            }
                        }
                    }
                    view.caseOnFocus=null;
                } else view.caseOnFocus=b;
            }
            verifFinDePartie();
        } else if(event.getSource().equals(view.retour)){
            clip.stop();
            menu.getView().setMainMenuView();
            menu.getView().getStage().getScene().getStylesheets().clear();
            menu.getView().getStage().getScene().getStylesheets().add(new File("css/menu_view.css").toURI().toString());
            menu.game = null;
        } else if(event.getSource().equals(view.recommencer)){
            menu.nouvellepartie();
        } else if(event.getSource().equals(view.bouton_volume)){
            model.mute = !model.mute;
            view.bouton_volume.setImage(new Image(new File("img/sound_"+(model.mute?"off":"on")+".png").toURI().toString()));
            if(model.mute) clip.stop();
            else clip.play();
        } else if(event.getSource().equals(view.reprendre)){
            model.pause = !model.pause;
            view.menu_pane.setVisible(model.pause);
        } else if (event.getSource().equals(view.sauvegarder)){
            String nomsave=view.popUpSetNomSauvegarde();
            if (!nomsave.equals("")){
                model.saveStation(nomsave);
            }
        } else if (event.getSource().equals(view.lanceContinue)) {
            model.lanceContinue = !model.lanceContinue;
            actualiseCases = false;
            view.lanceContinue.setDisable(true);
        }


        /* ACTUALISATION */
        if(actualiseCases) view.actualizeCases();
    }

    private void verifRenfortCapacite() {
        boolean ok=false;
        for(Case c:model.getJoueurCourant().getTerrain()) if((model.getMode()== Partie.CLASSICO && c.getNbtroupes()<24) || (model.getMode()== Partie.RAPIDO && c.getNbtroupes()<12)) ok=true;
        if(!ok) {
            model.passeEtapeSuivante();
            view.notice.setText(model.getJoueurCourant().getNom() + "\n" +
                    "Cliquez sur une de vos case puis sur une case adversaire ou neutre pour tenter de la conquérir");
            view.endTurn.setText("Terminer le tour");
        }
    }

    private void verifFinDePartie() {

        for (Joueur j:model.getJoueurs()){
            if (j.getTerrain().size()==0){
                j.setEliminated(true);
            }
        }

        if (model.nbjoueurRestant()==1){
            model.setFin(true);
            view.setFinDePartieView();
        }
    }
}