import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [FormsModule]
})
export class LoginComponent {
  isRegisterMode = signal<boolean>(false);
  http = inject(HttpClient);

  customerObj: any = {
    userId: 0,
    userName: '',
    emailId: '',
    fullName: '',
    password: '',
  };

  // Permet de basculer entre Login et Register
  toggleMode() {
    this.isRegisterMode.set(!this.isRegisterMode());
  }

  // Fonction d'inscription avec requête HTTP POST
  onRegister() {
    this.http.post("https://projectapi.gerasim.in/Bankloan/RegisterCustomer", this.customerObj)
      .subscribe({
        next: (res: any) => {
          if (res.result) {
            alert("Customer Registered Successfully! ✅");
          }
        },
        error: (err) => {
          alert("Network error ❌: " + err.message);
          console.error("Error details:", err);
        }
      });
  }
}



/* Que fait cette ligne ?

this.http.post(...) → Utilise HttpClient d'Angular pour envoyer une requête HTTP POST.
"https://projectapi.gerasim.in/Bankloan/RegisterCustomer" → L'URL du serveur où on envoie les données.
this.customerObj → Les données de l'utilisateur qu'on envoie au serveur (nom, email, mot de passe…).
 subscribe() :

Attend la réponse du serveur (réussi ou échec).
next: gère la réponse en cas de succès.
res: any contient la réponse du serveur.
🔹 Que se passe-t-il si l'inscription réussit ?

if (res.result) { alert("Customer Registered Successfully! ✅"); }
Si res.result (réponse du serveur)
 indique un succès, on affiche une alerte.
4️⃣ Gestion des erreurs
(ex: problème réseau, serveur en panne, etc.)
typescript
Copier
Modifier
  error: (err) => {
    alert("Network error ❌: " + err.message);
    console.error("Error details:", err);
  }
});
🔹 Si la requête échoue (ex: problème de connexion,
serveur indisponible) :

error: capture l'erreur.
alert("Network error ❌: " + err.message); → Affiche une alerte avec un message d'erreur.
console.error("Error details:", err); → Affiche les détails dans la console du navigateur (utile pour le débogage).






/*🔹 @Component → Déclare un composant Angular.
🔹 selector: 'app-login' → Permet d'utiliser <app-login></app-login> dans un autre fichier HTML.
🔹 templateUrl → Lien vers le fichier HTML (login.component.html).
🔹 s
/*🔹 @Component → Déclare un composant Angular.
🔹 selector: 'app-login' → Permet d'utiliser <app-login></app-login> dans un autre fichier HTML.
🔹 templateUrl → Lien vers le fichier HTML (login.component.html).
🔹 styleUrls → Lien vers le fichier CSS (login.component.css).*/`´
