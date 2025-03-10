import { Component, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  FormGroup,
  FormControl,
  ReactiveFormsModule,
  FormsModule,
} from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule],
})
export class LoginComponent {
  isRegisterMode = signal<boolean>(false);

  // ✅ Injection via le constructeur au lieu de `inject(HttpClient)`
  constructor(private http: HttpClient, private router: Router) {}

  customerObj = {
    userId: 0,
    userName: '',
    emailId: '',
    fullName: '',
    password: '',
  };

  loginForm = new FormGroup({
    userName: new FormControl(''),
    password: new FormControl(''),
  });

  toggleMode() {
    this.isRegisterMode.set(!this.isRegisterMode());
  }

  onRegister() {
    console.log("Tentative d'inscription...");

    this.http
      .post<any>(
        'https://projectapi.gerasim.in/Bankloan/RegisterCustomer',
        this.customerObj
      )
      .subscribe({
        next: (res: any) => {
          console.log('Réponse API:', res);
          if (res.result) {
            alert('Inscription réussie ✅');
          } else {
            alert(res.message);
          }
        },
        error: (err: any) => {
          alert('Erreur réseau ❌');
          console.error("Détails de l'erreur:", err);
        },
      });
  }

  onLogin() {
    console.log('Tentative de connexion...');

    this.http
      .post<any>(
        'https://projectapi.gerasim.in/api/BankLoan/login',
        this.loginForm.value
      )
      .subscribe({
        next: (res: any) => {
          if (res.result) {
            sessionStorage.setItem('bankerUser', JSON.stringify(res.data));
            this.router.navigateByUrl('application-list');
          } else {
            alert(res.message);
          }
        },
        error: (err: any) => {
          alert('Erreur réseau ❌');
          console.error("Détails de l'erreur:", err);
        },
      });
  }
}
