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

@Injectable({
  providedIn: 'root'
})
export class OperationsStoreService {
  private operations = signal<Operation[]>(this.loadFromLocalStorage());

  constructor() {
    console.log('📊 [OPERATIONS-STORE] Service initialisé');
  }

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
    return this.operations().filter(op =>
      op.numCompteSource === numCompte
    ).sort((a, b) =>
      new Date(b.dateOperation).getTime() - new Date(a.dateOperation).getTime()
    );
  }

  getRecentOperations(numCompte: string, limit: number = 5): Operation[] {
    return this.getOperationsByCompte(numCompte).slice(0, limit);
  }

  private addOperation(operation: Partial<Operation>) {
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

    console.log('✅ [OPERATIONS-STORE] Opération ajoutée:', newOperation);
    return newOperation;
  }

  addVirement(
    numCompteSource: string,
    numCompteDestinataire: string,
    montant: number,
    communication: string,
    intituleSource: string = 'Compte',
    intituleDestinataire: string = 'Compte'
  ) {
    this.addOperation({
      type: 'VIREMENT',
      montant: -montant,
      numCompteSource,
      numCompteDestinataire,
      communication,
      description: `Virement vers ${intituleDestinataire} (${numCompteDestinataire.slice(-4)})`
    });

    console.log('✅ [OPERATIONS-STORE] Virement enregistré');
  }

  addDepot(numCompte: string, montant: number, description: string = 'Dépôt en espèces') {
    this.addOperation({
      type: 'DEPOT',
      montant: montant,
      numCompteSource: numCompte,
      communication: 'Dépôt',
      description
    });

    console.log('✅ [OPERATIONS-STORE] Dépôt enregistré');
  }

  addDepotWithDate(
    numCompte: string, 
    montant: number, 
    description: string, 
    dateOperation: Date
  ) {
    this.addOperation({
      type: 'DEPOT',
      montant: montant,
      numCompteSource: numCompte,
      communication: 'Dépôt RDV',
      description,
      dateOperation: dateOperation.toISOString()
    });

    console.log('✅ [OPERATIONS-STORE] Dépôt RDV enregistré avec date:', dateOperation);
  }

  addRetrait(numCompte: string, montant: number, description: string = 'Retrait en espèces') {
    this.addOperation({
      type: 'RETRAIT',
      montant: -montant,
      numCompteSource: numCompte,
      communication: 'Retrait',
      description
    });

    console.log('✅ [OPERATIONS-STORE] Retrait enregistré');
  }

  addRetraitWithDate(
    numCompte: string, 
    montant: number, 
    description: string, 
    dateOperation: Date
  ) {
    this.addOperation({
      type: 'RETRAIT',
      montant: -montant,
      numCompteSource: numCompte,
      communication: 'Retrait RDV',
      description,
      dateOperation: dateOperation.toISOString()
    });

    console.log('✅ [OPERATIONS-STORE] Retrait RDV enregistré avec date:', dateOperation);
  }

  addFrais(numCompte: string, montant: number, description: string = 'Frais bancaires') {
    this.addOperation({
      type: 'FRAIS',
      montant: -montant,
      numCompteSource: numCompte,
      communication: 'Frais',
      description
    });

    console.log('✅ [OPERATIONS-STORE] Frais enregistrés');
  }

  clearAll() {
    this.operations.set([]);
    localStorage.removeItem('banking_operations');
    console.log('🗑️ [OPERATIONS-STORE] Toutes les opérations effacées');
  }
}