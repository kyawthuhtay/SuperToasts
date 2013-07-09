/**
 *  Copyright 2013 John Persano
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 * 
 */

package com.extlibsupertoasts;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

/** Manages the life of a SuperActivityToast. Copied from the Crouton library */
public class ManagerSuperActivityToast extends Handler {

	private static final class Messages {

		public static final int DISPLAY_SUPERACTIVITYTOAST = 0xc2007;
		public static final int ADD_SUPERACTIVITYTOAST = 0xc20074dd;
		public static final int REMOVE_SUPERACTIVITYTOAST = 0xc2007de1;

		private Messages() {

			// Do nothing

		}

	}

	private static ManagerSuperActivityToast manager;

	private Queue<SuperActivityToast> toastQueue;

	private ManagerSuperActivityToast() {

		toastQueue = new LinkedBlockingQueue<SuperActivityToast>();

	}

	protected static synchronized ManagerSuperActivityToast getInstance() {

		if (manager != null) {

			return manager;

		} else {

			manager = new ManagerSuperActivityToast();

			return manager;

		}

	}
	

	protected void add(SuperActivityToast superActivityToast) {

		toastQueue.add(superActivityToast);
		this.showNextSuperToast();

	}

	
	private void showNextSuperToast() {

		if (toastQueue.isEmpty()) {

			return;

		}

		final SuperActivityToast superActivityToast = toastQueue.peek();

		if (superActivityToast.getActivity() == null) {

			toastQueue.poll();

		}

		if (!superActivityToast.isShowing()) {

			sendMessage(superActivityToast, Messages.ADD_SUPERACTIVITYTOAST);

		} else {

			sendMessageDelayed(superActivityToast,
					Messages.DISPLAY_SUPERACTIVITYTOAST,
					getDuration(superActivityToast));

		}

	}

	
	private void sendMessage(SuperActivityToast superActivityToast,
			final int messageId) {

		final Message message = obtainMessage(messageId);
		message.obj = superActivityToast;
		sendMessage(message);

	}

	
	private void sendMessageDelayed(SuperActivityToast superActivityToast,
			final int messageId, final long delay) {

		Message message = obtainMessage(messageId);
		message.obj = superActivityToast;
		sendMessageDelayed(message, delay);

	}

	
	private long getDuration(SuperActivityToast superActivityToast) {

		long duration = superActivityToast.getDuration();
		duration += superActivityToast.getShowAnimation().getDuration();
		duration += superActivityToast.getDismissAnimation().getDuration();

		return duration;

	}

	@Override
	public void handleMessage(Message message) {

		final SuperActivityToast superActivityToast = (SuperActivityToast) 
				message.obj;

		switch (message.what) {

			case Messages.DISPLAY_SUPERACTIVITYTOAST:
	
				showNextSuperToast();
	
				break;
	
			case Messages.ADD_SUPERACTIVITYTOAST:
	
				displaySuperToast(superActivityToast);
	
				break;
	
			case Messages.REMOVE_SUPERACTIVITYTOAST:
	
				removeSuperToast(superActivityToast);
	
				break;
	
			default: {
	
				super.handleMessage(message);
	
				break;

			}

		}

	}

	private void displaySuperToast(SuperActivityToast superActivityToast) {

		if (superActivityToast.isShowing()) {

			return;

		}

		final ViewGroup viewGroup = superActivityToast.getViewGroup();

		final View toastView = superActivityToast.getView();
		
		if(viewGroup != null) {
			
			viewGroup.addView(toastView);

			toastView.startAnimation(superActivityToast.getShowAnimation());
		}

		
		if(!superActivityToast.getIsIndeterminate()) {
			
			sendMessageDelayed(superActivityToast, Messages.REMOVE_SUPERACTIVITYTOAST,
					superActivityToast.getDuration() + superActivityToast.getShowAnimation().getDuration());
			
		}

	}

	
	protected void removeSuperToast(SuperActivityToast superActivityToast) {

		final ViewGroup viewGroup = superActivityToast.getViewGroup();

		final View toastView = superActivityToast.getView();

		if (viewGroup != null) {

			toastView.startAnimation(superActivityToast.getDismissAnimation());

			toastQueue.poll();

			viewGroup.removeView(toastView);

			sendMessageDelayed(superActivityToast,
					Messages.DISPLAY_SUPERACTIVITYTOAST, superActivityToast
							.getDismissAnimation().getDuration());
			
			if(superActivityToast.getOnDismissListener() != null) {
				
				superActivityToast.getOnDismissListener().onDismiss();
				
			}

		}

	}
	

	protected void clearQueue() {

		removeAllMessages();

		if (toastQueue != null) {

			for (SuperActivityToast superActivityToast : toastQueue) {

				if (superActivityToast.isShowing()) {

					superActivityToast.getViewGroup().removeView(
							superActivityToast.getView());

				}

			}

			toastQueue.clear();

		}

	}

	protected void clearSuperActivityToastsForActivity(Activity activity) {

		if (toastQueue != null) {

			Iterator<SuperActivityToast> superActivityToastIterator = toastQueue
					.iterator();

			while (superActivityToastIterator.hasNext()) {

				SuperActivityToast superActivityToast = superActivityToastIterator
						.next();

				if ((superActivityToast.getActivity()) != null
						&& superActivityToast.getActivity().equals(activity)) {

					if (superActivityToast.isShowing()) {

						superActivityToast.getViewGroup().removeView(
								superActivityToast.getView());

					}

					removeAllMessagesForSuperActivityToast(superActivityToast);

					superActivityToastIterator.remove();

				}

			}

		}

	}

	
	private void removeAllMessages() {

		removeMessages(Messages.ADD_SUPERACTIVITYTOAST);
		removeMessages(Messages.DISPLAY_SUPERACTIVITYTOAST);
		removeMessages(Messages.REMOVE_SUPERACTIVITYTOAST);

	}

	
	private void removeAllMessagesForSuperActivityToast(
			SuperActivityToast superActivityToast) {
		removeMessages(Messages.ADD_SUPERACTIVITYTOAST, superActivityToast);
		removeMessages(Messages.DISPLAY_SUPERACTIVITYTOAST, superActivityToast);
		removeMessages(Messages.REMOVE_SUPERACTIVITYTOAST, superActivityToast);

	}

}
