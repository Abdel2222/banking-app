import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http'; // Import ajouté
import { appRoutes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(appRoutes),
    provideHttpClient(), // Fonction maintenant correctement importée
  ],
};
/*ApplicationConfig → Interface Angular utilisée pour définir la configuration globale de l’application.
provideRouter(appRoutes) → Ajoute le système de routage Angular en utilisant la liste de routes appRoutes.
provideHttpClient() → Ajoute le service HTTP pour effectuer des requêtes HTTP (GET, POST, PUT, etc.).
appRoutes → C’est un tableau de routes définissant les chemins de navigation.
2️⃣ Définition de appConfig
appConfig est l’objet de configuration de l’application.
Il contient un tableau providers qui fournit des services utilisés dans l’application.
📌 Que fait chaque service ?

provideRouter(appRoutes)

Active le routage et enregistre les routes définies dans app.routes.ts.
Permet de naviguer entre les composants avec routerLink et RouterModule.
provideHttpClient()

Active le module HTTP d’Angular pour effectuer des requêtes API.
Permet d’utiliser HttpClient dans les services Angular.
✅ Pourquoi utiliser app.config.ts dans Angular 19 ?
Dans les anciennes versions d’Angular, les services étaient définis dans app.module.ts en utilisant NgModule.
Depuis Angular 15+, on peut remplacer les modules par une configuration basée sur les providers.

🚀 Avantages de app.config.ts :

✅ Meilleure optimisation → Pas besoin de NgModule, ce qui réduit la complexité.
✅ Lazy Loading plus performant → L'application charge uniquement ce dont elle a besoin.
✅ Configuration plus claire et modulaire.
🎯 Résumé de la logique
Importer les services nécessaires (Router, HttpClient).
Définir appConfig pour fournir ces services à toute l’application.
Angular utilise appConfig pour activer le routage et les requêtes HTTP dès le démarrage.
