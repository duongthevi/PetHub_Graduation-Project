export default function AdminPageHeader({ eyebrow = "Quản trị PetHub", title, description, action }) {
  return <div className="admin-page-header"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1>{description && <p>{description}</p>}</div>{action}</div>;
}
