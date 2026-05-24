import { Injectable, signal } from '@angular/core';

export interface Operation {
  id: number;
  type: string;
  montant: number;
  dateOperation: string;
  numCompteSource?: string;
  numCompteDestinataire?: string;
  communication?: string;
  description?: string;
  statut?: string;
}

@Injectable({ providedIn: 'root' })
export class OperationsStoreService {
  private operations = signal<Operation[]>(this.loadFromLocalStorage());

  private loadFromLocalStorage(): Operation[] {
    try {
      const stored = localStorage.getItem('banking_operations');
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private saveToLocalStorage(operations: Operation[]) {
    localStorage.setItem('banking_operations', JSON.stringify(operations));
  }

  getAllOperations(): Operation[] {
    return this.operations();
  }

  getOperationsByCompte(numCompte: string): Operation[] {
    return this.operations()
      .filter(op => op.numCompteSource === numCompte)
      .sort((a, b) =>
        new Date(b.dateOperation).getTime() - new Date(a.dateOperation).getTime()
      );
  }

  getRecentOperations(numCompte: string, limit = 5): Operation[] {
    return this.getOperationsByCompte(numCompte).slice(0, limit);
  }

  private addOperation(operation: Partial<Operation>): Operation {
    const newOperation: Operation = {
      id: Date.now() + Math.random(),
      type: operation.type || 'VIREMENT',
      montant: operation.montant || 0,
      dateOperation: operation.dateOperation || new Date().toISOString(),
      numCompteSource: operation.numCompteSource,
      numCompteDestinataire: operation.numCompteDestinataire,
      communication: operation.communication,
      description: operation.description,
      statut: 'COMPLETED'
    };

    const updated = [newOperation, ...this.operations()];
    this.operations.set(updated);
    this.saveToLocalStorage(updated);
    return newOperation;
  }

  /* ====== Virements ====== */

  addVirement(
    numCompteSource: string,
    numCompteDestinataire: string,
    montant: number,
    communication: string,
    intituleSource = 'Compte',
    intituleDestinataire = 'Compte'
  ) {
    this.addOperation({
      type: 'VIREMENT',
      montant: -montant,
      numCompteSource,
      numCompteDestinataire,
      communication,
      description: `Virement vers ${intituleDestinataire} (${numCompteDestinataire.slice(-4)})`
    });
  }

  /* ====== Dépôts / Retraits ====== */

  addDepot(numCompte: string, montant: number, description = 'Dépôt en espèces') {
    this.addOperation({
      type: 'DEPOT', montant, numCompteSource: numCompte,
      communication: 'Dépôt', description
    });
  }

  addDepotWithDate(numCompte: string, montant: number, description: string, dateOperation: Date) {
    this.addOperation({
      type: 'DEPOT', montant, numCompteSource: numCompte,
      communication: 'Dépôt RDV', description,
      dateOperation: dateOperation.toISOString()
    });
  }

  addRetrait(numCompte: string, montant: number, description = 'Retrait en espèces') {
    this.addOperation({
      type: 'RETRAIT', montant: -montant, numCompteSource: numCompte,
      communication: 'Retrait', description
    });
  }

  addRetraitWithDate(numCompte: string, montant: number, description: string, dateOperation: Date) {
    this.addOperation({
      type: 'RETRAIT', montant: -montant, numCompteSource: numCompte,
      communication: 'Retrait RDV', description,
      dateOperation: dateOperation.toISOString()
    });
  }

  addFrais(numCompte: string, montant: number, description = 'Frais bancaires') {
    this.addOperation({
      type: 'FRAIS', montant: -montant, numCompteSource: numCompte,
      communication: 'Frais', description
    });
  }

  /* ====== Placements ✅ NOUVEAU ====== */

  /**
   * Débit lors de la création d'un placement.
   */
  addPlacement(numCompte: string, montant: number, nomFonds: string) {
    this.addOperation({
      type: 'PLACEMENT',
      montant: -montant,  // débit du compte
      numCompteSource: numCompte,
      communication: 'Placement',
      description: `Placement dans ${nomFonds}`
    });
  }

  /**
   * Crédit lors d'une clôture normale (montant + gain).
   */
  addCloturePlacement(numCompte: string, montantRestitue: number, nomFonds: string) {
    this.addOperation({
      type: 'CLOTURE_PLACEMENT',
      montant: +montantRestitue,  // crédit du compte
      numCompteSource: numCompte,
      communication: 'Clôture placement',
      description: `Clôture placement ${nomFonds} — montant + gain restitués`
    });
  }

  /**
   * Crédit lors d'une sortie anticipée (montant - frais, gain perdu).
   */
  addSortieAnticipee(
    numCompte: string,
    montantRestitue: number,
    fraisSortie: number,
    nomFonds: string
  ) {
    // Crédit du montant restitué
    this.addOperation({
      type: 'SORTIE_ANTICIPEE',
      montant: +montantRestitue,
      numCompteSource: numCompte,
      communication: 'Sortie anticipée',
      description: `Sortie anticipée ${nomFonds} — restitué: ${montantRestitue.toFixed(2)} €`
    });

    // Débit des frais de sortie
    if (fraisSortie > 0) {
      this.addOperation({
        type: 'FRAIS_SORTIE',
        montant: -fraisSortie,
        numCompteSource: numCompte,
        communication: 'Frais sortie anticipée',
        description: `Frais de sortie anticipée ${nomFonds}`
      });
    }
  }

  clearAll() {
    this.operations.set([]);
    localStorage.removeItem('banking_operations');
  }
}