import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-new-loan-form',
  standalone: true, // Important pour les composants standalone
  templateUrl: './new-loan-form.component.html',
  styleUrls: ['./new-loan-form.component.css'],
  imports: [ReactiveFormsModule], // Ajout du module ici ✅
})
export class NewLoanFormComponent {
  loanForm: FormGroup;
  http = inject(HttpClient);

  constructor(private fb: FormBuilder) {
    this.loanForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      bankName: ['', Validators.required],
      loanAmount: ['', Validators.required],
      emi: ['', Validators.required],
    });
  }

  saveLoan() {
    if (this.loanForm.valid) {
      this.http
        .post('https://api.example.com/loan', this.loanForm.value)
        .subscribe({
          next: (res) => alert('Loan application submitted successfully ✅'),
          error: (err) => alert('Error submitting loan application ❌'),
        });
    } else {
      alert('Please fill all required fields ❌');
    }
  }
}
