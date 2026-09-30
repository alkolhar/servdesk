import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslocoDirective } from '@jsverse/transloco';

export interface CommentDialogData {
  action: string;
}

/** Asks for the comment an action requires; closes with the text, or undefined if cancelled. */
@Component({
  selector: 'app-comment-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TranslocoDirective,
  ],
  template: `
    <ng-container *transloco="let t">
      <h2 mat-dialog-title>{{ t('action.' + data.action) }}</h2>
      <form [formGroup]="form" (ngSubmit)="confirm()">
        <mat-dialog-content>
          <p>{{ t('ticket.comment.why.' + data.action) }}</p>
          <mat-form-field appearance="outline" class="comment">
            <mat-label>{{ t('ticket.comment.label') }}</mat-label>
            <textarea matInput formControlName="comment" rows="4" required></textarea>
            <mat-error>{{ t('form.required') }}</mat-error>
          </mat-form-field>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
          <button mat-button type="button" mat-dialog-close>{{ t('ticket.comment.back') }}</button>
          <button mat-flat-button type="submit">{{ t('action.' + data.action) }}</button>
        </mat-dialog-actions>
      </form>
    </ng-container>
  `,
  styles: `
    .comment {
      width: 100%;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommentDialog {
  protected readonly data = inject<CommentDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CommentDialog, string>);

  protected readonly form = inject(NonNullableFormBuilder).group({
    // whitespace-only is no comment: the server would refuse it too
    comment: ['', [Validators.required, Validators.pattern(/\S/)]],
  });

  protected confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue().comment.trim());
  }
}
