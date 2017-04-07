package edu.tum.uc.jvm.extractor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FileDescriptorExtractor implements IExtractor {
	private Map<String, String> _return;

	public FileDescriptorExtractor(){
		_return = new HashMap<String,String>();
	}
	/**
	 * Extracts the file descriptor from object parameter obj
	 * and add it to the mapping
	 * @param obj
	 * @return
	 */
	@Override
	public Map<?,?> extract(Object obj) {
		_return.clear();
		String osSystem = System.getProperty("os.name");
		try {
			if ((obj instanceof Writer) || (obj instanceof Reader)) {
				List<java.lang.reflect.Field> attrs = getAllFields(obj);
				Iterator<java.lang.reflect.Field> it = attrs.iterator();
				while (it.hasNext()) {
					java.lang.reflect.Field field = it.next();
					// Find attribute "lock" -> FileOutputStream
					if ("lock".equals(field.getName().toLowerCase())) {
						field.setAccessible(true);
						Object lock = field.get(obj);
						if (lock instanceof FileOutputStream) {
							java.util.List<java.lang.reflect.Field> attrs2 = getAllFields(lock);
							Iterator<java.lang.reflect.Field> it2 = attrs2.iterator();
							while (it2.hasNext()) {
								field = it2.next();
								// Find attribute "fd" -> FileDescriptor of lock
								if ("fd".equals(field.getName().toLowerCase())) {
									field.setAccessible(true);
									Object fd = field.get(lock);
									if (fd instanceof FileDescriptor) {
										java.util.List<java.lang.reflect.Field> attrs3 = getAllFields(fd);
										Iterator<java.lang.reflect.Field> it3 = attrs3.iterator();
										// Find attribue "int fd" -> contains
										// the file descriptor id from os
										while (it3.hasNext()) {
											field = it3.next();
											if ("fd".equals(field.getName().toLowerCase())) {
												field.setAccessible(true);
												Object fdOs = field.get(fd);
												if (fdOs instanceof Integer) {
													// System.out.println("Mirror
													// fdOs: "+fdOs);
													_return.put("fd", String.valueOf(fdOs));
//													fileDescriptor = String.valueOf(fdOs);
												}
											}
											if ("handle".equals(field.getName().toLowerCase())
													&& osSystem.toLowerCase().contains("window")) {
												field.setAccessible(true);
												Object handleOs = field.get(fd);
												if (handleOs instanceof Long) {
													_return.put("handle", String.valueOf(handleOs));
//													handle = String.valueOf(handleOs);
												}
											}
										}
									}
								}
							}
						}
						else if (lock instanceof Reader){
							_return.putAll((Map<? extends String, ? extends String>) this.extract(lock));
						}
					}
				}
			} else if ((obj instanceof FileOutputStream) || (obj instanceof FileInputStream)) {
				java.util.List<java.lang.reflect.Field> attrs2 = getAllFields(obj);
				Iterator<java.lang.reflect.Field> it2 = attrs2.iterator();
				while (it2.hasNext()) {
					java.lang.reflect.Field field = it2.next();
					// Find attribute "fd" -> FileDescriptor of lock
					if ("fd".equals(field.getName().toLowerCase())) {
						field.setAccessible(true);
						Object fd = field.get(obj);
						if (fd instanceof FileDescriptor) {
							java.util.List<java.lang.reflect.Field> attrs3 = getAllFields(fd);
							Iterator<java.lang.reflect.Field> it3 = attrs3.iterator();
							// Find attribue "int fd" -> contains
							// the file descriptor id from os
							while (it3.hasNext()) {
								field = it3.next();
								if ("fd".equals(field.getName().toLowerCase())) {
									field.setAccessible(true);
									Object fdOs = field.get(fd);
									if (fdOs instanceof Integer) {
										// System.out.println("Mirror fdOs:
										// "+fdOs);
										_return.put("fd", String.valueOf(fdOs));
//										fileDescriptor = String.valueOf(fdOs);
									}
								}

								if ("handle".equals(field.getName().toLowerCase())
										&& osSystem.toLowerCase().contains("window")) {
									field.setAccessible(true);
									Object handleOs = field.get(fd);
									if (handleOs instanceof Long) {
										_return.put("handle", String.valueOf(handleOs));
//										handle = String.valueOf(handleOs);
									}
								}
							}
						}
					}
				}
			} else if ((obj instanceof FilterOutputStream) || (obj instanceof FilterInputStream)) {
				List<java.lang.reflect.Field> attrs = getAllFields(obj);
				Iterator<java.lang.reflect.Field> it = attrs.iterator();
				while (it.hasNext()) {
					java.lang.reflect.Field field = it.next();
					// Find attribute "lock" -> FileOutputStream
					if ("out".equals(field.getName().toLowerCase()) || "in".equals(field.getName().toLowerCase())) {
						field.setAccessible(true);
						Object out = field.get(obj);
						if ((out instanceof FileOutputStream) || (out instanceof FileInputStream)) {
							java.util.List<java.lang.reflect.Field> attrs2 = getAllFields(out);
							Iterator<java.lang.reflect.Field> it2 = attrs2.iterator();
							while (it2.hasNext()) {
								field = it2.next();
								// Find attribute "fd" -> FileDescriptor of lock
								if ("fd".equals(field.getName().toLowerCase())) {
									field.setAccessible(true);
									Object fd = field.get(out);
									if (fd instanceof FileDescriptor) {
										java.util.List<java.lang.reflect.Field> attrs3 = getAllFields(fd);
										Iterator<java.lang.reflect.Field> it3 = attrs3.iterator();
										// Find attribue "int fd" -> contains
										// the file descriptor id from os
										while (it3.hasNext()) {
											field = it3.next();
											if ("fd".equals(field.getName().toLowerCase())) {
												field.setAccessible(true);
												Object fdOs = field.get(fd);
												if (fdOs instanceof Integer) {
													// System.out.println("Mirror
													// fdOs: "+fdOs);
													_return.put("fd", String.valueOf(fdOs));
//													fileDescriptor = String.valueOf(fdOs);
												}
											}

											if ("handle".equals(field.getName().toLowerCase())
													&& osSystem.toLowerCase().contains("window")) {
												field.setAccessible(true);
												Object handleOs = field.get(fd);
												if (handleOs instanceof Long) {
													_return.put("handle", String.valueOf(handleOs));
//													handle = String.valueOf(handleOs);
												}
											}
										}
									}
								}
								else if ("path".equals(field.getName().toLowerCase())) {
									field.setAccessible(true);
									Object path = field.get(out);
									if (path instanceof String) {
										_return.put("path", String.valueOf(path));
//										handle = String.valueOf(handleOs);
									}
								}
							}
						}
					}
				}
			} else if (obj instanceof File){
				List<java.lang.reflect.Field> attrs = getAllFields(obj);
				Iterator<java.lang.reflect.Field> it = attrs.iterator();
				while (it.hasNext()) {
					java.lang.reflect.Field field = it.next();
					// Find attribute "lock" -> FileOutputStream
					if ("path".equals(field.getName().toLowerCase())) {
						field.setAccessible(true);
						Object path = field.get(obj);
						if(path instanceof String){
							_return.put("path", String.valueOf(path));
						}
					}
				}
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if ("-1".equals(_return.get("fd")) || "".equals(_return.get("fd"))) {
			if (!"-1".equals(_return.get("handle")) && !"".equals(_return.get("handle"))) {
				_return.put("fd", _return.get("handle"));
			}
		}
		return _return;
	}
	
//	return all field from an object
	private List<Field> getAllFields(Object obj) {
		ArrayList<Field> _return = new ArrayList<Field>();
		Class<?> clazz = obj.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field f : clazz.getDeclaredFields())
				_return.add(f);
			clazz = clazz.getSuperclass();
		}
		return _return;
	}

}
