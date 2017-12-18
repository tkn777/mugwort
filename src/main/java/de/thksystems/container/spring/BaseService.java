/*
 * tksCommons / mugwort
 * 
 * Author : Thomas Kuhlmann (ThK-Systems, http://oss.thk-systems.de) License : LGPL (https://www.gnu.org/licenses/lgpl.html)
 */
package de.thksystems.container.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import de.thksystems.util.function.CheckedRunnable;
import de.thksystems.util.function.CheckedSupplier;

public abstract class BaseService extends BaseComponent {

	private final static Logger LOG = LoggerFactory.getLogger(BaseService.class);

	@Autowired
	private PlatformTransactionManager transactionManager;

	/**
	 * Start transaction manually.
	 * <p>
	 * <b>Use it with caution!</b><br>
	 * <b>Do not mix annotation based transaction handling and programmatically one!</b><br>
	 * 
	 * @param readonly
	 *            For readonly transactions.
	 * @param propagation
	 *            For special propagations (like REQUIRES_NEW)
	 */
	protected TransactionStatus startTransaction(boolean readonly, Propagation propagation) {
		LOG.trace("Starting new transaction");
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
		transactionDefinition.setReadOnly(readonly);
		transactionDefinition.setPropagationBehavior(propagation.value());
		return transactionManager.getTransaction(transactionDefinition);
	}

	/**
	 * Start transaction manually.
	 * <p>
	 * <b>Use it with caution!</b><br>
	 * <b>Do not mix annotation based transaction handling and programmatically one!</b><br>
	 * 
	 * @param readonly
	 *            For readonly transactions.
	 */
	protected TransactionStatus startTransaction(boolean readonly) {
		return startTransaction(readonly, Propagation.REQUIRED);
	}

	/**
	 * Start transaction manually.
	 * <p>
	 * <b>Use it with caution!</b><br>
	 * <b>Do not mix annotation based transaction handling and programmatically one!</b><br>
	 * 
	 * @param propagation
	 *            For special propagations (like REQUIRES_NEW)
	 */
	protected TransactionStatus startTransaction(Propagation propagation) {
		return startTransaction(false, propagation);
	}

	/**
	 * Start transaction manually.
	 * <p>
	 * <b>Use it with caution!</b><br>
	 * <b>Do not mix annotation based transaction handling and programmatically one!</b><br>
	 */
	protected TransactionStatus startTransaction() {
		return startTransaction(false, Propagation.REQUIRED);
	}

	/**
	 * Start transacation manually, if required.
	 *
	 * @see #startTransaction()
	 */
	protected TransactionStatus startTransactionIfRequired(TransactionStatus transactionStatus) {
		if (transactionStatus == null || transactionStatus.isCompleted()) {
			return startTransaction();
		}
		return transactionStatus;
	}

	/**
	 * Commit transaction manually.
	 * <p>
	 * If the transaction is set rollbackOnly, a rollback is done.
	 * 
	 * @see #startTransaction() for notes and warning.
	 */
	protected void commitTransaction(TransactionStatus transactionStatus) {
		if (transactionStatus != null && !transactionStatus.isCompleted()) {
			if (transactionStatus.isRollbackOnly()) {
				LOG.trace("Transaction is set rollback only.");
				rollbackTransaction(transactionStatus);
			} else {
				LOG.trace("Commiting transaction");
				transactionManager.commit(transactionStatus);
				LOG.trace("Transaction commited");
			}
		}
	}

	/**
	 * Rollback transaction manually.
	 * 
	 * @see #startTransaction() for notes and warning.
	 */
	protected void rollbackTransaction(TransactionStatus transactionStatus) {
		if (transactionStatus != null && !transactionStatus.isCompleted()) {
			LOG.trace("Rolling back transaction.");
			transactionManager.rollback(transactionStatus);
			LOG.trace("Transaction rollbacked.");
		}
	}

	/**
	 * Runs in transaction.
	 */
	protected <X extends Throwable> void runInTransaction(CheckedRunnable<X> runnable) throws X {
		runInTransaction(Propagation.REQUIRED, runnable);
	}

	/**
	 * Runs in transaction.
	 */
	protected <X extends Throwable> void runInTransaction(Propagation propagation, CheckedRunnable<X> runnable) throws X {
		TransactionStatus transactionStatus = null;
		try {
			transactionStatus = startTransaction(propagation);
			runnable.run();
			commitTransaction(transactionStatus);
		} catch (Throwable t) { // NOSONAR
			rollbackTransaction(transactionStatus);
			throw t;
		}
	}

	/**
	 * Runs in transaction.
	 */
	protected <R, X extends Throwable> R runInTransaction(CheckedSupplier<R, X> supplier) throws X {
		return runInTransaction(Propagation.REQUIRED, supplier);
	}

	/**
	 * Runs in transaction.
	 */
	protected <R, X extends Throwable> R runInTransaction(Propagation propagation, CheckedSupplier<R, X> supplier) throws X {
		TransactionStatus transactionStatus = null;
		try {
			transactionStatus = startTransaction(propagation);
			R result = supplier.get();
			commitTransaction(transactionStatus);
			return result;
		} catch (Throwable t) { // NOSONAR
			rollbackTransaction(transactionStatus);
			throw t;
		}
	}

}