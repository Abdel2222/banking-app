import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { OperationsService } from '../../core/services/operations.service';
import { OperationsStoreService } from '../../core/services/operations-store.service';

interface TimeSlot {
  time: string;
  available: boolean;
}

interface CalendarDay {
  date: Date;
  day: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isPast: boolean;
  isSelected: boolean;
  isWeekend: boolean;
}

@Component({
  selector: 'app-rendez-vous',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './rendez-vous.component.html',
  styleUrls: ['./rendez-vous.component.scss']
})
export class RendezVousComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private operationsService = inject(OperationsService);
  private operationsStore = inject(OperationsStoreService);

  montant = signal<number>(0);
  minMontant = signal<number>(0);
  typeOperation = signal<string>('');
  numCompte = signal<string>('');
  success = signal(false);

  currentMonth = signal<Date>(new Date());
  calendarDays = signal<CalendarDay[]>([]);
  selectedDate = signal<Date | null>(null);
  selectedTime = signal<string | null>(null);

  timeSlots = signal<TimeSlot[]>([]);

  rdvForm = this.fb.group({
    nom: ['', [Validators.required, Validators.minLength(2)]],
    prenom: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    telephone: [''],
    agence: ['Bruxelles Centre - Grand Place', Validators.required],
    motif: ['', [
      Validators.required,
      Validators.minLength(10),
      Validators.pattern(/^[A-Za-zÀ-ÿ0-9\s,.'!?-]{10,}$/)
    ]],
    montant: [0, [Validators.required, Validators.min(0.01)]]
  });

  agences = [
    'Bruxelles Centre - Grand Place',
    'Bruxelles Nord - Gare du Nord',
    'Bruxelles Sud - Avenue Louise',
    'Bruxelles Est - Woluwe',
    'Bruxelles Ouest - Anderlecht',
    'Ixelles - Flagey',
    'Uccle - Altitude 100',
    'Schaerbeek - Place Colignon'
  ];

  ngOnInit() {
    const user = this.authService.currentUser();
    console.log('[RDV] User connecté:', user);

    if (user) {
      const [prenom, ...nomParts] = (user.nomComplet || '').split(' ');
      const nom = nomParts.join(' ');

      this.rdvForm.patchValue({
        prenom: prenom || '',
        nom: nom || '',
        email: user.email || ''
      });
    }

    const montant = this.route.snapshot.queryParams['montant'];
    const type = this.route.snapshot.queryParams['type'];
    const numCompte = this.route.snapshot.queryParams['numCompte'];
    const minMontant = this.route.snapshot.queryParams['minMontant'];

    if (montant) {
      this.montant.set(Number(montant));
      this.rdvForm.patchValue({ montant: Number(montant) });
    }
    if (type) {
      this.typeOperation.set(type);
    }
    if (numCompte) {
      this.numCompte.set(numCompte);
    }
    if (minMontant) {
      this.minMontant.set(Number(minMontant));

      if (!montant || Number(montant) < Number(minMontant)) {
        this.rdvForm.patchValue({ montant: Number(minMontant) });
      }

      this.rdvForm.get('montant')?.setValidators([
        Validators.required,
        Validators.min(Number(minMontant))
      ]);
      this.rdvForm.get('montant')?.updateValueAndValidity();
    }

    this.generateCalendar();
    this.generateTimeSlots();
  }

  generateCalendar() {
    const year = this.currentMonth().getFullYear();
    const month = this.currentMonth().getMonth();

    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());

    const endDate = new Date(lastDay);
    endDate.setDate(endDate.getDate() + (6 - lastDay.getDay()));

    const days: CalendarDay[] = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const current = new Date(startDate);
    while (current <= endDate) {
      const dayDate = new Date(current);
      dayDate.setHours(0, 0, 0, 0);

      const isSelected = this.selectedDate() ? dayDate.getTime() === this.selectedDate()!.getTime() : false;
      const isWeekend = current.getDay() === 0 || current.getDay() === 6;

      days.push({
        date: new Date(current),
        day: current.getDate(),
        isCurrentMonth: current.getMonth() === month,
        isToday: dayDate.getTime() === today.getTime(),
        isPast: dayDate < today,
        isSelected: isSelected,
        isWeekend: isWeekend
      });

      current.setDate(current.getDate() + 1);
    }

    this.calendarDays.set(days);
  }

  generateTimeSlots() {
    const slots: TimeSlot[] = [];
    const morningSlots = ['09:00', '09:30', '10:00', '10:30', '11:00', '11:30'];
    const afternoonSlots = ['14:00', '14:30', '15:00', '15:30', '16:00', '16:30'];

    const now = new Date();
    const selectedDate = this.selectedDate();
    const isToday = selectedDate &&
                    selectedDate.toDateString() === now.toDateString();

    [...morningSlots, ...afternoonSlots].forEach(time => {
      let available = Math.random() > 0.3;

      if (isToday) {
        const [hours, minutes] = time.split(':').map(Number);
        const slotTime = new Date(now);
        slotTime.setHours(hours, minutes, 0, 0);

        if (slotTime <= now) {
          available = false;
        }
      }

      slots.push({ time, available });
    });

    this.timeSlots.set(slots);
  }

  previousMonth() {
    const current = this.currentMonth();
    this.currentMonth.set(new Date(current.getFullYear(), current.getMonth() - 1, 1));
    this.generateCalendar();
  }

  nextMonth() {
    const current = this.currentMonth();
    this.currentMonth.set(new Date(current.getFullYear(), current.getMonth() + 1, 1));
    this.generateCalendar();
  }

  selectDate(day: CalendarDay) {
    if (day.isPast || !day.isCurrentMonth || day.isWeekend) return;

    this.selectedDate.set(day.date);
    this.selectedTime.set(null);
    this.generateCalendar();
    this.generateTimeSlots();
  }

  selectTime(slot: TimeSlot) {
    if (!slot.available) return;
    this.selectedTime.set(slot.time);
  }

  canSubmit(): boolean {
    return this.rdvForm.valid &&
           this.selectedDate() !== null &&
           this.selectedTime() !== null;
  }

  submitRDV() {
    if (!this.canSubmit()) {
      this.rdvForm.markAllAsTouched();
      return;
    }

    const rdvData = {
      ...this.rdvForm.value,
      date: this.selectedDate(),
      heure: this.selectedTime(),
      typeOperation: this.typeOperation(),
      numCompte: this.numCompte()
    };

    console.log('[RDV] Rendez-vous demandé:', rdvData);

    const montantOperation = rdvData.montant!;
    const numCompte = this.numCompte();
    const typeOp = this.typeOperation();

    // Construire la date complète du RDV (date + heure)
    const dateRDV = new Date(this.selectedDate()!);
    const [heures, minutes] = this.selectedTime()!.split(':');
    dateRDV.setHours(parseInt(heures), parseInt(minutes), 0, 0);

    const description = `${typeOp} en agence - ${rdvData.agence} - RDV le ${this.formatSelectedDate()} à ${this.selectedTime()}`;

    // EFFECTUER L'OPÉRATION BANCAIRE
    let operationObservable;

    if (typeOp === 'DÉPÔT') {
      operationObservable = this.operationsService.deposit(
        numCompte,
        montantOperation,
        description
      );
    } else if (typeOp === 'RETRAIT') {
      operationObservable = this.operationsService.withdraw(
        numCompte,
        montantOperation,
        description
      );
    }

    if (operationObservable) {
      operationObservable.subscribe({
        next: (response) => {
          console.log('[RDV] Opération bancaire réussie:', response);

          // ENREGISTRER DANS LE STORE LOCAL
          if (typeOp === 'DÉPÔT') {
            this.operationsStore.addDepotWithDate(
              numCompte,
              montantOperation,
              description,
              dateRDV
            );
          } else if (typeOp === 'RETRAIT') {
            this.operationsStore.addRetraitWithDate(
              numCompte,
              montantOperation,
              description,
              dateRDV
            );
          }

          // ENVOI DE LA NOTIFICATION
          this.notificationService.addRDVNotification(
            this.formatSelectedDate(),
            this.selectedTime()!,
            this.typeOperation(),
            rdvData.agence!,
            rdvData.montant!
          );

          console.log('[RDV] Notification envoyée au service');

          this.success.set(true);

          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 3000);
        },
        error: (err) => {
          console.error('[RDV] Erreur lors de l\'opération:', err);
          alert('Erreur lors de l\'opération bancaire. Veuillez réessayer.');
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(montant);
  }

  getMonthYear(): string {
    const months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
                    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
    return `${months[this.currentMonth().getMonth()]} ${this.currentMonth().getFullYear()}`;
  }

  formatSelectedDate(): string {
    if (!this.selectedDate()) return '';
    const date = this.selectedDate()!;
    return date.toLocaleDateString('fr-FR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getAgenceAddress(agence: string): string {
    const addresses: { [key: string]: string } = {
      'Bruxelles Centre - Grand Place': 'Grand Place 1, 1000 Bruxelles',
      'Bruxelles Nord - Gare du Nord': 'Rue du Progrès 80, 1210 Bruxelles',
      'Bruxelles Sud - Avenue Louise': 'Avenue Louise 250, 1050 Bruxelles',
      'Bruxelles Est - Woluwe': 'Avenue de Tervueren 2, 1150 Woluwe-Saint-Pierre',
      'Bruxelles Ouest - Anderlecht': 'Rue de la Loi 15, 1070 Anderlecht',
      'Ixelles - Flagey': 'Place Eugène Flagey 18, 1050 Ixelles',
      'Uccle - Altitude 100': 'Chaussée de Waterloo 1485, 1180 Uccle',
      'Schaerbeek - Place Colignon': 'Place Colignon 1, 1030 Schaerbeek'
    };
    return addresses[agence] || 'Adresse non disponible';
  }

  get montantErrorMessage(): string {
    const control = this.rdvForm.get('montant');
    if (control?.hasError('required')) {
      return 'Le montant est requis';
    }
    if (control?.hasError('min')) {
      return `Le montant minimum pour un ${this.typeOperation()} est de ${this.formatMontant(this.minMontant())}`;
    }
    return '';
  }

  get motifErrorMessage(): string {
    const control = this.rdvForm.get('motif');
    if (control?.hasError('required')) {
      return 'Le motif est requis';
    }
    if (control?.hasError('minlength')) {
      return 'Le motif doit contenir au moins 10 caractères';
    }
    if (control?.hasError('pattern')) {
      return 'Veuillez décrire votre motif en quelques mots';
    }
    return '';
  }
}
